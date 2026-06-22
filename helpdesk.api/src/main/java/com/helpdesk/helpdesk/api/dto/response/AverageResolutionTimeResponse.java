package com.helpdesk.helpdesk.api.dto.response;

public class AverageResolutionTimeResponse {

    private long resolvedTicketCount;
    private Double averageResolutionHours;

    public AverageResolutionTimeResponse() {
    }

    public AverageResolutionTimeResponse(long resolvedTicketCount, Double averageResolutionHours) {
        this.resolvedTicketCount = resolvedTicketCount;
        this.averageResolutionHours = averageResolutionHours;
    }

    public long getResolvedTicketCount() {
        return resolvedTicketCount;
    }

    public void setResolvedTicketCount(long resolvedTicketCount) {
        this.resolvedTicketCount = resolvedTicketCount;
    }

    public Double getAverageResolutionHours() {
        return averageResolutionHours;
    }

    public void setAverageResolutionHours(Double averageResolutionHours) {
        this.averageResolutionHours = averageResolutionHours;
    }
}
