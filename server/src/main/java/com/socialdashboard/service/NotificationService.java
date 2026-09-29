package com.socialdashboard.service;

import com.socialdashboard.dto.Dtos;
import com.socialdashboard.entity.Notification;
import com.socialdashboard.entity.Notification.NotificationType;
import com.socialdashboard.entity.User;
import com.socialdashboard.exception.ApiException;
import com.socialdashboard.repository.NotificationRepository;
import com.socialdashboard.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<Dtos.NotificationResponse> list(Long userId, int page, int size, String typeRaw, Boolean unreadOnly) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        if (unreadOnly != null && unreadOnly) {
            // Unread-only view is small by nature, so page the filtered set directly.
            List<Dtos.NotificationResponse> unread = notificationRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, pageable).stream()
                    .filter(n -> !n.isRead())
                    .map(Dtos.NotificationResponse::from)
                    .toList();
            return new PageImpl<>(unread, pageable, unread.size());
        }

        if (typeRaw != null && !typeRaw.isBlank() && !"all".equalsIgnoreCase(typeRaw)) {
            NotificationType type = parseType(typeRaw);
            return new PageImpl<>(
                    notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable).stream()
                            .map(Dtos.NotificationResponse::from).toList(),
                    pageable,
                    notificationRepository.countByUserIdAndType(userId, type));
        }

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(Dtos.NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public List<Dtos.NotificationResponse> unread(Long userId) {
        return notificationRepository.findTop10ByUserIdAndReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(Dtos.NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public Dtos.NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = requireOwned(userId, notificationId);
        notification.setRead(true);
        notificationRepository.save(notification);
        return Dtos.NotificationResponse.from(notification);
    }

    @Transactional
    public long markAllRead(Long userId) {
        List<Notification> all = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, 500)).getContent();
        all.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(all);
        return all.size();
    }

    @Transactional
    public void create(Long userId, NotificationType type, String title, String message, String link) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        notificationRepository.save(new Notification(user, type, title, message, link));
    }

    private Notification requireOwned(Long userId, Long notificationId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Notification"));
    }

    private static NotificationType parseType(String raw) {
        try {
            return NotificationType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Unknown notification type: " + raw, "type");
        }
    }
}
