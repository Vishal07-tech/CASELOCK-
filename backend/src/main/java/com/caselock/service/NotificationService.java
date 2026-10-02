package com.caselock.service;

import com.caselock.dto.response.NotificationResponse;
import com.caselock.dto.response.PageResponse;
import com.caselock.entity.Notification;
import com.caselock.entity.User;
import com.caselock.entity.enums.NotificationType;
import com.caselock.exception.ResourceNotFoundException;
import com.caselock.repository.NotificationRepository;
import com.caselock.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void notify(Long recipientId, NotificationType type, String title, String message,
                        Long relatedCaseId, Long relatedEvidenceId) {
        if (recipientId == null) {
            return;
        }
        User recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient == null) {
            return;
        }
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .relatedCaseId(relatedCaseId)
                .relatedEvidenceId(relatedEvidenceId)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listForUser(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found.");
        }
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(Long userId) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 500);
        Page<Notification> page = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        page.getContent().forEach(n -> n.setRead(true));
        notificationRepository.saveAll(page.getContent());
    }
}
