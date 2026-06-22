package com.helpdesk.helpdesk.api.dto.response;

import com.helpdesk.helpdesk.api.entity.enums.Category;
import com.helpdesk.helpdesk.api.entity.enums.Priority;
import com.helpdesk.helpdesk.api.entity.enums.TicketStatus;

import java.time.LocalDateTime;

public class TicketSummaryResponse {

    private Long id;
    private String title;
    private Priority priority;
    private Category category;
    private TicketStatus status;
    private String raisedByName;
    private String assignedAgentName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TicketSummaryResponse() {
    }

    public TicketSummaryResponse(Long id, String title, Priority priority, Category category, TicketStatus status,
                                  String raisedByName, String assignedAgentName,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.priority = priority;
        this.category = category;
        this.status = status;
        this.raisedByName = raisedByName;
        this.assignedAgentName = assignedAgentName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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

    public String getRaisedByName() {
        return raisedByName;
    }

    public void setRaisedByName(String raisedByName) {
        this.raisedByName = raisedByName;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
