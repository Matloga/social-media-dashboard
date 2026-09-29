package com.socialdashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialdashboard.dto.Dtos;
import com.socialdashboard.repository.MetricSnapshotRepository;
import com.socialdashboard.repository.PostRepository;
import com.socialdashboard.repository.SocialAccountRepository;
import com.socialdashboard.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.seed.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
})
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SocialAccountRepository accountRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MetricSnapshotRepository metricRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private com.socialdashboard.service.GoalService goalService;

    private String token;
    private String email;

    @BeforeEach
    void setUp() throws Exception {
        // Unique per test so the shared in-memory database never collides.
        email = "t" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "password123",
                                "name", "Test User"))))
                .andExpect(status().isCreated())
                .andReturn();

        token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + token);
    }

    @Test
    @DisplayName("rejects registration with a duplicate email")
    void duplicateEmailRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "password123", "name", "Dupe"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("rejects a short password")
    void shortPasswordRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "new" + UUID.randomUUID() + "@example.com",
                                "password", "short",
                                "name", "Nope"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("rejects bad credentials")
    void badLoginRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "wrongpassword"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("blocks protected routes without a token")
    void unauthenticatedBlocked() throws Exception {
        mockMvc.perform(get("/api/stats/overview"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("blocks a malformed token")
    void malformedTokenBlocked() throws Exception {
        mockMvc.perform(get("/api/stats/overview").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("returns the current profile")
    void returnsProfile() throws Exception {
        mockMvc.perform(authed(get("/api/auth/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("connects an account and backfills history")
    void connectAccount() throws Exception {
        MvcResult result = mockMvc.perform(authed(post("/api/accounts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("platform", "INSTAGRAM", "username", "testcreator"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.platform").value("INSTAGRAM"))
                .andExpect(jsonPath("$.username").value("testcreator"))
                .andReturn();

        long accountId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertThat(accountId).isPositive();

        // A connected account must immediately have chart data behind it.
        mockMvc.perform(authed(get("/api/stats/timeseries").param("range", "30d")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(30))
                .andExpect(jsonPath("$.points[29].followers").isNumber());
    }

    @Test
    @DisplayName("rejects a duplicate account for the same platform")
    void duplicateAccountRejected() throws Exception {
        Map<String, String> body = Map.of("platform", "X", "username", "dupehandle");
        mockMvc.perform(authed(post("/api/accounts"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated());
        mockMvc.perform(authed(post("/api/accounts"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("overview cards are internally consistent")
    void overviewConsistency() throws Exception {
        connectAccount();

        MvcResult result = mockMvc.perform(authed(get("/api/stats/overview")).param("range", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards").isArray())
                .andReturn();

        var cards = objectMapper.readTree(result.getResponse().getContentAsString()).get("cards");
        assertThat(cards).isNotEmpty();
        assertThat(cards.findValuesAsText("key")).contains("followers", "likes", "comments", "engagement");

        cards.forEach(card -> {
            String key = card.get("key").asText();
            double value = card.get("value").asDouble();
            assertThat(value).as("card %s value", key).isGreaterThanOrEqualTo(0);
            assertThat(card.get("delta").get("direction").asText())
                    .as("card %s direction", key).isIn("up", "down", "flat");
        });
    }

    @Test
    @DisplayName("time series returns one point per day in the window")
    void timeSeriesShape() throws Exception {
        connectAccount();

        for (String range : new String[]{"1d", "7d", "30d", "90d", "365d", "45d"}) {
            mockMvc.perform(authed(get("/api/stats/timeseries")).param("range", range))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.points").isArray());
        }

        MvcResult result = mockMvc.perform(authed(get("/api/stats/timeseries")).param("range", "7d"))
                .andExpect(status().isOk())
                .andReturn();

        var points = objectMapper.readTree(result.getResponse().getContentAsString()).get("points");
        assertThat(points).hasSize(7);
        assertThat(points.get(0).get("date").asText()).isEqualTo(LocalDate.now().minusDays(6).toString());
        assertThat(points.get(6).get("date").asText()).isEqualTo(LocalDate.now().toString());
    }

    @Test
    @DisplayName("platform filter narrows the results")
    void platformFilter() throws Exception {
        connectAccount();
        mockMvc.perform(authed(post("/api/accounts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("platform", "TIKTOK", "username", "tiktoktest"))))
                .andExpect(status().isCreated());

        mockMvc.perform(authed(get("/api/stats/overview"))
                        .param("range", "30d").param("platform", "TIKTOK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byPlatform", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.byPlatform[0].platform").value("TIKTOK"));

        mockMvc.perform(authed(get("/api/stats/overview")).param("platform", "NOTAREALPLATFORM"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("paginates and filters posts")
    void postPaginationAndFilters() throws Exception {
        connectAccount();

        mockMvc.perform(authed(get("/api/posts")).param("size", "5").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());

        mockMvc.perform(authed(get("/api/posts")).param("platform", "INSTAGRAM").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].platform").value("INSTAGRAM"));

        mockMvc.perform(authed(get("/api/posts")).param("type", "NOTATYPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("does not leak another user's data")
    void tenantIsolation() throws Exception {
        MvcResult created = mockMvc.perform(authed(post("/api/accounts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("platform", "FACEBOOK", "username", "mine"))))
                .andExpect(status().isCreated())
                .andReturn();
        long myAccountId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // Second user, separate account.
        String otherEmail = "o" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        MvcResult other = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", otherEmail, "password", "password123", "name", "Other"))))
                .andExpect(status().isCreated())
                .andReturn();
        String otherToken = objectMapper.readTree(other.getResponse().getContentAsString()).get("token").asText();

        // The other user must not see or touch the first user's account.
        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));

        mockMvc.perform(get("/api/posts").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(authed(get("/api/posts")).param("accountId", String.valueOf(myAccountId)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("exports a CSV report")
    void csvExport() throws Exception {
        connectAccount();
        MvcResult result = mockMvc.perform(authed(get("/api/reports/export.csv")).param("range", "30d"))
                .andExpect(status().isOk())
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        assertThat(csv).contains("Metric,Value").contains("Total Reach").contains("Daily Trend");
        assertThat(result.getResponse().getHeader("Content-Disposition")).contains("attachment");
    }

    @Test
    @DisplayName("generates a report for each range")
    void reportRanges() throws Exception {
        connectAccount();
        for (String range : new String[]{"1d", "7d", "30d", "90d", "365d"}) {
            mockMvc.perform(authed(get("/api/stats/reports")).param("range", range))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rangeLabel").isNotEmpty())
                    .andExpect(jsonPath("$.from").isNotEmpty())
                    .andExpect(jsonPath("$.to").isNotEmpty());
        }
    }

    @Test
    @DisplayName("manages notifications")
    void notifications() throws Exception {
        mockMvc.perform(authed(get("/api/notifications/unread-count")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").isNumber());

        mockMvc.perform(authed(post("/api/notifications/read-all")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("updates the profile and refuses a wrong current password")
    void profileUpdate() throws Exception {
        mockMvc.perform(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/auth/profile"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Renamed", "bio", "Updated bio"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.bio").value("Updated bio"));

        mockMvc.perform(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/auth/profile"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("newPassword", "brandnewpass", "currentPassword", "notmypass"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("disconnects an account and removes its data")
    void disconnectAccount() throws Exception {
        MvcResult created = mockMvc.perform(authed(post("/api/accounts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("platform", "YOUTUBE", "username", "todisconnect"))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(authed(get("/api/posts")).param("size", "1"))
                .andExpect(jsonPath("$.totalElements").isNumber());

        mockMvc.perform(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/accounts/" + id)))
                .andExpect(status().isNoContent());

        assertThat(accountRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("re-syncing an account is idempotent and leaves no orphan hashtags")
    void resyncIsIdempotent() throws Exception {
        MvcResult created = mockMvc.perform(authed(post("/api/accounts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("platform", "YOUTUBE", "username", "resyncme"))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        long postsAfterConnect = postRepository.count();
        long snapshotsAfterConnect = metricRepository.count();
        long hashtagsAfterConnect = hashtagRowCount();

        // The generated history overlaps the existing one, so this used to trip
        // the unique (account, stat_date) index and return a 500.
        mockMvc.perform(authed(post("/api/accounts/" + id + "/sync")))
                .andExpect(status().isOk());

        assertThat(postRepository.count())
                .as("sync must replace the posts, not append to them")
                .isEqualTo(postsAfterConnect);
        assertThat(metricRepository.count())
                .as("sync must replace the snapshots, not append to them")
                .isEqualTo(snapshotsAfterConnect);
        assertThat(hashtagRowCount())
                .as("the post_hashtags join table must be cleaned up with the posts")
                .isEqualTo(hashtagsAfterConnect);

        // Sync-all walks every connected account, so it has to stay idempotent too.
        mockMvc.perform(authed(post("/api/accounts/sync")))
                .andExpect(status().isOk());
        assertThat(postRepository.count()).isEqualTo(postsAfterConnect);
        assertThat(metricRepository.count()).isEqualTo(snapshotsAfterConnect);
    }

    @Test
    @DisplayName("disconnecting an account clears its hashtags")
    void disconnectClearsHashtags() throws Exception {
        MvcResult created = mockMvc.perform(authed(post("/api/accounts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("platform", "FACEBOOK", "username", "tagcleanup"))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        long before = hashtagRowCount();
        assertThat(before).isPositive();

        mockMvc.perform(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/accounts/" + id)))
                .andExpect(status().isNoContent());

        assertThat(hashtagRowCount())
                .as("a bulk post delete would leave the join table rows behind")
                .isLessThan(before);
    }

    // ---------- goals ----------

    @Test
    @DisplayName("creates a goal and reports live progress")
    void goalLifecycle() throws Exception {
        mockMvc.perform(authed(post("/api/accounts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("platform", "INSTAGRAM", "username", "goaltester"))))
                .andExpect(status().isCreated());

        MvcResult created = mockMvc.perform(authed(post("/api/goals"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "metric", "FOLLOWERS",
                                "title", "Reach 1K followers",
                                "targetValue", 1000,
                                "period", "MONTHLY"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metric").value("FOLLOWERS"))
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.progress").isNumber())
                .andExpect(jsonPath("$.daysTotal").value(30))
                .andReturn();

        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(authed(get("/api/goals")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(authed(put("/api/goals/" + id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("targetValue", 500, "active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetValue").value(500))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/goals/" + id)))
                .andExpect(status().isNoContent());

        mockMvc.perform(authed(get("/api/goals")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("rejects a goal with a non-positive target")
    void goalRejectsBadTarget() throws Exception {
        mockMvc.perform(authed(post("/api/goals"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("metric", "FOLLOWERS", "targetValue", 0))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(authed(post("/api/goals"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("metric", "FOLLOWERS", "targetValue", -5))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("rejects a goal with an unknown metric")
    void goalRejectsUnknownMetric() throws Exception {
        mockMvc.perform(authed(post("/api/goals"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("metric", "NOT_A_METRIC", "targetValue", 10))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("keeps goals private between users")
    void goalTenantIsolation() throws Exception {
        mockMvc.perform(authed(post("/api/accounts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("platform", "TIKTOK", "username", "goalowner"))))
                .andExpect(status().isCreated());

        MvcResult created = mockMvc.perform(authed(post("/api/goals"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("metric", "LIKES", "targetValue", 1000))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        String auth = "Bearer " + registerOtherUser();

        mockMvc.perform(get("/api/goals").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(put("/api/goals/" + id)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("targetValue", 1))))
                .andExpect(status().isNotFound());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/goals/" + id).header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("sums the follower baseline across every connected account")
    void goalBaselineCoversAllAccounts() throws Exception {
        // Two accounts, so a baseline that only looked at the first row of the
        // result set would report roughly half the audience.
        for (String username : List.of("baselineone", "baselinetwo")) {
            mockMvc.perform(authed(post("/api/accounts"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("platform", "INSTAGRAM", "username", username))))
                    .andExpect(status().isCreated());
        }

        MvcResult created = mockMvc.perform(authed(post("/api/goals"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("metric", "FOLLOWERS", "targetValue", 1_000_000))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        Long uid = userId();
        Dtos.GoalResponse goal = goalService.list(uid).stream()
                .filter(g -> g.id().equals(id)).findFirst().orElseThrow();

        List<com.socialdashboard.entity.MetricSnapshot> window = metricRepository
                .findWindowAscending(uid, goal.startDate(), goal.endDate(), null);

        // Recompute the expected values: each account's earliest day, summed.
        // A baseline taken from the first row alone would miss every other account.
        java.util.Map<Long, com.socialdashboard.entity.MetricSnapshot> earliest = new java.util.LinkedHashMap<>();
        for (com.socialdashboard.entity.MetricSnapshot row : window) {
            earliest.merge(row.getAccount().getId(), row,
                    (a, b) -> a.getStatDate().isAfter(b.getStatDate()) ? b : a);
        }
        long expectedBaseline = earliest.values().stream()
                .mapToLong(com.socialdashboard.entity.MetricSnapshot::getFollowers).sum();
        long singleRowBaseline = window.get(0).getFollowers();

        assertThat(earliest).as("test needs more than one account to be meaningful").hasSizeGreaterThan(1);
        assertThat(expectedBaseline).isNotEqualTo(singleRowBaseline);
        assertThat(goal.baselineValue())
                .as("baseline must sum every account, not use the first row")
                .isEqualTo(expectedBaseline);

        // The account is generated to grow, so the latest day must be at or above
        // the earliest. This catches picking the wrong end of the window.
        java.util.Map<Long, com.socialdashboard.entity.MetricSnapshot> latest = new java.util.LinkedHashMap<>();
        for (com.socialdashboard.entity.MetricSnapshot row : window) {
            latest.merge(row.getAccount().getId(), row,
                    (a, b) -> a.getStatDate().isBefore(b.getStatDate()) ? b : a);
        }
        long expectedCurrent = latest.values().stream()
                .mapToLong(com.socialdashboard.entity.MetricSnapshot::getFollowers).sum();
        assertThat(goal.currentValue()).isEqualTo(expectedCurrent);
        assertThat(goal.currentValue())
                .as("a growing account must not read as smaller at the end of the window")
                .isGreaterThanOrEqualTo(goal.baselineValue());
    }

    private Long userId() {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
    }

    // ---------- insights ----------

    @Test
    @DisplayName("returns insights with evidence once data exists")
    void insightsAfterConnecting() throws Exception {
        mockMvc.perform(authed(get("/api/insights")).param("range", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("No data yet"));

        mockMvc.perform(authed(post("/api/accounts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("platform", "INSTAGRAM", "username", "insightuser"))))
                .andExpect(status().isCreated());

        mockMvc.perform(authed(get("/api/insights")).param("range", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(1)))
                .andExpect(jsonPath("$[0].severity").isString())
                .andExpect(jsonPath("$[0].title").isString())
                .andExpect(jsonPath("$[0].detail").isNotEmpty());
    }

    @Test
    @DisplayName("insights honour the platform filter and reject junk")
    void insightsFilters() throws Exception {
        mockMvc.perform(authed(post("/api/accounts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("platform", "LINKEDIN", "username", "insightfilter"))))
                .andExpect(status().isCreated());

        mockMvc.perform(authed(get("/api/insights")).param("platform", "LINKEDIN"))
                .andExpect(status().isOk());
        mockMvc.perform(authed(get("/api/insights")).param("platform", "NOPE"))
                .andExpect(status().isBadRequest());
    }

    /** Registers a second user and returns their token. */
    private String registerOtherUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "other" + UUID.randomUUID() + "@example.com",
                                "password", "password123",
                                "name", "Other User"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("reports an unknown endpoint as 404, not 500")
    void unknownRouteIsNotFound() throws Exception {
        mockMvc.perform(authed(get("/api/accounts/1/history")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // Security runs before routing, so an unauthenticated request to an
        // unknown path is still a 401 and not a 404.
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(authed(get("/api/does-not-exist")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("reports a wrong HTTP verb as 405, not 500")
    void wrongMethodIsNotAllowed() throws Exception {
        // The profile route is PUT-only.
        mockMvc.perform(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/auth/profile"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Nope"))))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("rejects a malformed JSON body as 400, not 500")
    void malformedBodyIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());
    }

    /** Counts rows in the generated post_hashtags join table. */
    private long hashtagRowCount() {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM post_hashtags")
                .getSingleResult()).longValue();
    }
}
