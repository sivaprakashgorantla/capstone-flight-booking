package com.flight.notification_service.repository;

import com.flight.notification_service.model.Notification;
import com.flight.notification_service.model.NotificationStatus;
import com.flight.notification_service.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByNotificationReference(String reference);

    /** All notifications for a user, newest first */
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    /** Unread (PENDING or SENT) notifications for a user */
    List<Notification> findByUserIdAndStatusNotOrderByCreatedAtDesc(String userId, NotificationStatus status);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByType(NotificationType type);

    List<Notification> findAllByOrderByCreatedAtDesc();

    /** Count unread (non-READ) notifications for a user */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.status <> 'READ'")
    long countUnreadByUserId(@Param("userId") String userId);

    boolean existsByNotificationReference(String reference);
}
