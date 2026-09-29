package com.socialdashboard.repository;

import com.socialdashboard.entity.MetricSnapshot;
import com.socialdashboard.entity.SocialAccount.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MetricSnapshotRepository extends JpaRepository<MetricSnapshot, Long> {

    @Query("""
            SELECT m FROM MetricSnapshot m
            WHERE m.account.user.id = :userId
              AND m.statDate BETWEEN :from AND :to
              AND (:platform IS NULL OR m.account.platform = :platform)
            ORDER BY m.statDate ASC, m.account.platform ASC
            """)
    List<MetricSnapshot> findRange(@Param("userId") Long userId,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to,
                                   @Param("platform") Platform platform);

    @Query("""
            SELECT m FROM MetricSnapshot m
            WHERE m.account.user.id = :userId
              AND m.statDate = :date
              AND (:platform IS NULL OR m.account.platform = :platform)
            """)
    List<MetricSnapshot> findOnDate(@Param("userId") Long userId,
                                    @Param("date") LocalDate date,
                                    @Param("platform") Platform platform);

    @Query("""
            SELECT m FROM MetricSnapshot m
            WHERE m.account.id = :accountId AND m.statDate BETWEEN :from AND :to
            ORDER BY m.statDate ASC
            """)
    List<MetricSnapshot> findForAccount(@Param("accountId") Long accountId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);

    /**
     * Bulk delete, deliberately annotated with flush/clear flags. A derived
     * delete would queue the removals in the persistence context, and Hibernate
     * flushes inserts before deletes, so re-syncing an account would collide
     * with the unique (account, date) index on the freshly generated rows.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM MetricSnapshot m WHERE m.account.id = :accountId")
    int deleteByAccountId(@Param("accountId") Long accountId);

    /** First and last daily values inside a window, used for goal progress. */
    @Query("""
            SELECT m FROM MetricSnapshot m
            WHERE m.account.user.id = :userId
              AND m.statDate BETWEEN :from AND :to
              AND (:platform IS NULL OR m.account.platform = :platform)
            ORDER BY m.statDate ASC, m.account.id ASC
            """)
    List<MetricSnapshot> findWindowAscending(@Param("userId") Long userId,
                                            @Param("from") LocalDate from,
                                            @Param("to") LocalDate to,
                                            @Param("platform") Platform platform);
}
