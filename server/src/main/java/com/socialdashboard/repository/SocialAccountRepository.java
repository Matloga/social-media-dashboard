package com.socialdashboard.repository;

import com.socialdashboard.entity.SocialAccount;
import com.socialdashboard.entity.SocialAccount.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    List<SocialAccount> findByUserIdOrderByFollowersDesc(Long userId);

    Optional<SocialAccount> findByIdAndUserId(Long id, Long userId);

    List<SocialAccount> findByUserIdAndPlatform(Long userId, Platform platform);

    boolean existsByUserIdAndPlatformAndUsernameIgnoreCase(Long userId, Platform platform, String username);

    long countByUserId(Long userId);
}
