package com.socialdashboard.repository;

import com.socialdashboard.entity.Post;
import com.socialdashboard.entity.Post.PostType;
import com.socialdashboard.entity.SocialAccount.Platform;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
            SELECT p FROM Post p
            WHERE p.user.id = :userId
              AND (:platform IS NULL OR p.platform = :platform)
              AND (:type IS NULL OR p.type = :type)
              AND (:accountId IS NULL OR p.account.id = :accountId)
            """)
    Page<Post> search(@Param("userId") Long userId,
                      @Param("platform") Platform platform,
                      @Param("type") PostType type,
                      @Param("accountId") Long accountId,
                      Pageable pageable);

    @Query("""
            SELECT p FROM Post p
            WHERE p.user.id = :userId
              AND (:platform IS NULL OR p.platform = :platform)
              AND (:type IS NULL OR p.type = :type)
            """)
    List<Post> findTopByUserForChart(@Param("userId") Long userId,
                                     @Param("platform") Platform platform,
                                     @Param("type") PostType type,
                                     Pageable pageable);

    long countByUserId(Long userId);

    @Query("SELECT p FROM Post p WHERE p.user.id = :userId ORDER BY p.publishedAt DESC")
    List<Post> findRecent(@Param("userId") Long userId, Pageable pageable);

    /**
     * Derived delete on purpose. A bulk JPQL delete removes the post rows but
     * leaves the post_hashtags join table behind, because element collections
     * are not touched by bulk operations. Loading the entities lets Hibernate
     * clean the collection rows up as well.
     */
    void deleteAllByAccountId(Long accountId);

    @Query("""
            SELECT COALESCE(AVG(m.reach), 0) FROM MetricSnapshot m
            WHERE m.account.user.id = :userId AND m.statDate >= :from
              AND (:platform IS NULL OR m.account.platform = :platform)
            """)
    double averageReach(@Param("userId") Long userId,
                        @Param("from") java.time.LocalDate from,
                        @Param("platform") Platform platform);
}
