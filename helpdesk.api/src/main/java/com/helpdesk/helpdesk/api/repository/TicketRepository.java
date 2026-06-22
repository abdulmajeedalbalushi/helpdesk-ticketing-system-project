package com.helpdesk.helpdesk.api.repository;

import com.helpdesk.helpdesk.api.entity.Ticket;
import com.helpdesk.helpdesk.api.entity.enums.Category;
import com.helpdesk.helpdesk.api.entity.enums.Priority;
import com.helpdesk.helpdesk.api.entity.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("""
            SELECT t FROM Ticket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:category IS NULL OR t.category = :category)
              AND (:assignedAgentId IS NULL OR t.assignedAgent.id = :assignedAgentId)
            """)
    List<Ticket> search(@Param("status") TicketStatus status,
                         @Param("priority") Priority priority,
                         @Param("category") Category category,
                         @Param("assignedAgentId") Long assignedAgentId);

    @Query("""
            SELECT t FROM Ticket t
            WHERE t.resolvedAt IS NOT NULL
              AND (:assignedAgentId IS NULL OR t.assignedAgent.id = :assignedAgentId)
              AND (:category IS NULL OR t.category = :category)
            """)
    List<Ticket> findResolvedTickets(@Param("assignedAgentId") Long assignedAgentId,
                                      @Param("category") Category category);

    List<Ticket> findByStatusNot(TicketStatus status);
}
