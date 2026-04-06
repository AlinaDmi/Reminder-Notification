package com.example.reminder.repository;

import com.example.reminder.entity.ReminderEntity;
import com.example.reminder.entity.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ReminderRepository extends JpaRepository<ReminderEntity, Long> {

    List<ReminderEntity> findAllByStatusOrderByScheduledAtAsc(ReminderStatus status);

    @Query("""
            select r
            from ReminderEntity r
            where r.status in :statuses
              and r.scheduledAt <= :now
              and (r.nextRetryAt is null or r.nextRetryAt <= :now)
            order by r.scheduledAt asc
            """)
    List<ReminderEntity> findDueForDispatch(
            @Param("statuses") Collection<ReminderStatus> statuses,
            @Param("now") Instant now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update ReminderEntity r
            set r.status = :toStatus,
                r.updatedAt = :updatedAt
            where r.id = :id
              and r.status in :fromStatuses
            """)
    int transitionStatus(
            @Param("id") Long id,
            @Param("fromStatuses") Collection<ReminderStatus> fromStatuses,
            @Param("toStatus") ReminderStatus toStatus,
            @Param("updatedAt") Instant updatedAt
    );
}
