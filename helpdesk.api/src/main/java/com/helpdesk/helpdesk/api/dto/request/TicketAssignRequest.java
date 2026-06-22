package com.helpdesk.helpdesk.api.dto.request;

import jakarta.validation.constraints.NotNull;

public class TicketAssignRequest {

    @NotNull(message = "agentId is required")
    private Long agentId;

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }
}
