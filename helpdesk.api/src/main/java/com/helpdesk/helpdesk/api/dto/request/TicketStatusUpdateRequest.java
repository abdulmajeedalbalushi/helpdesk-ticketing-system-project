package com.helpdesk.helpdesk.api.dto.request;

import com.helpdesk.helpdesk.api.entity.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public class TicketStatusUpdateRequest {

    @NotNull(message = "status is required")
    private TicketStatus status;

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }
}
