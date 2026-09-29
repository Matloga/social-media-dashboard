package com.socialdashboard.repository;

import com.socialdashboard.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByUserIdOrderByActiveDescCreatedAtDesc(Long userId);

    List<Goal> findByUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);
}
