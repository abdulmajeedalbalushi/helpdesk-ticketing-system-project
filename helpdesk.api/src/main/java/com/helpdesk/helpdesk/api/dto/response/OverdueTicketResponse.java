package com.helpdesk.helpdesk.api.dto.response;

import com.helpdesk.helpdesk.api.entity.enums.Category;
import com.helpdesk.helpdesk.api.entity.enums.Priority;
import com.helpdesk.helpdesk.api.entity.enums.TicketStatus;

import java.time.LocalDateTime;

public class OverdueTicketResponse {

    private Long id;
    private String title;
    private Priority priority;
    private Category category;
    private TicketStatus status;
    private String assignedAgentName;
    private LocalDateTime createdAt;
    private long slaHours;
    private long hoursOpen;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getAssignedAgentName() {
        return assignedAgentName;
    }

    public void setAssignedAgentName(String assignedAgentName) {
        this.assignedAgentName = assignedAgentName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getSlaHours() {
        return slaHours;
    }

    public void setSlaHours(long slaHours) {
        this.slaHours = slaHours;
    }

    public long getHoursOpen() {
        return hoursOpen;
    }

    public void setHoursOpen(long hoursOpen) {
        this.hoursOpen = hoursOpen;
    }
}
