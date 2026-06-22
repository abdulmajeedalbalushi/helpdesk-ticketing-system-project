package com.helpdesk.helpdesk.api.controller;

import com.helpdesk.helpdesk.api.dto.request.CommentCreateRequest;
import com.helpdesk.helpdesk.api.dto.request.TicketAssignRequest;
import com.helpdesk.helpdesk.api.dto.request.TicketCreateRequest;
import com.helpdesk.helpdesk.api.dto.request.TicketStatusUpdateRequest;
import com.helpdesk.helpdesk.api.dto.response.AverageResolutionTimeResponse;
import com.helpdesk.helpdesk.api.dto.response.OverdueTicketResponse;
import com.helpdesk.helpdesk.api.dto.response.TicketDetailResponse;
import com.helpdesk.helpdesk.api.dto.response.TicketSummaryResponse;
import com.helpdesk.helpdesk.api.entity.enums.Category;
import com.helpdesk.helpdesk.api.entity.enums.Priority;
import com.helpdesk.helpdesk.api.entity.enums.TicketStatus;
import com.helpdesk.helpdesk.api.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketDetailResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        TicketDetailResponse response = ticketService.createTicket(request);
        return ResponseEntity.created(URI.create("/api/tickets/" + response.getId())).body(response);
    }

    @GetMapping("/{id}")
    public TicketDetailResponse getTicket(@PathVariable Long id) {
        return ticketService.getTicket(id);
    }

    @GetMapping
    public List<TicketSummaryResponse> listTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Long assignedAgentId) {
        return ticketService.listTickets(status, priority, category, assignedAgentId);
    }

    @GetMapping("/reports/avg-resolution-time")
    public AverageResolutionTimeResponse getAverageResolutionTime(
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) Category category) {
        return ticketService.getAverageResolutionTime(agentId, category);
    }

    @GetMapping("/reports/overdue")
    public List<OverdueTicketResponse> getOverdueTickets() {
        return ticketService.getOverdueTickets();
    }

    @PostMapping("/{id}/assign")
    public TicketDetailResponse assignAgent(@PathVariable Long id, @Valid @RequestBody TicketAssignRequest request) {
        return ticketService.assignAgent(id, request);
    }

    @PatchMapping("/{id}/status")
    public TicketDetailResponse updateStatus(@PathVariable Long id, @Valid @RequestBody TicketStatusUpdateRequest request) {
        return ticketService.updateStatus(id, request);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TicketDetailResponse> addComment(@PathVariable Long id, @Valid @RequestBody CommentCreateRequest request) {
        TicketDetailResponse response = ticketService.addComment(id, request);
        return ResponseEntity.status(201).body(response);
    }
}
