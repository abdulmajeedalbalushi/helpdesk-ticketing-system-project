package com.helpdesk.helpdesk.api.service;

import com.helpdesk.helpdesk.api.dto.request.CommentCreateRequest;
import com.helpdesk.helpdesk.api.dto.request.TicketAssignRequest;
import com.helpdesk.helpdesk.api.dto.request.TicketCreateRequest;
import com.helpdesk.helpdesk.api.dto.request.TicketStatusUpdateRequest;
import com.helpdesk.helpdesk.api.dto.response.AverageResolutionTimeResponse;
import com.helpdesk.helpdesk.api.dto.response.CommentResponse;
import com.helpdesk.helpdesk.api.dto.response.OverdueTicketResponse;
import com.helpdesk.helpdesk.api.dto.response.TicketDetailResponse;
import com.helpdesk.helpdesk.api.dto.response.TicketSummaryResponse;
import com.helpdesk.helpdesk.api.entity.Agent;
import com.helpdesk.helpdesk.api.entity.Comment;
import com.helpdesk.helpdesk.api.entity.Ticket;
import com.helpdesk.helpdesk.api.entity.User;
import com.helpdesk.helpdesk.api.entity.enums.AuthorType;
import com.helpdesk.helpdesk.api.entity.enums.Category;
import com.helpdesk.helpdesk.api.entity.enums.Priority;
import com.helpdesk.helpdesk.api.entity.enums.TicketStatus;
import com.helpdesk.helpdesk.api.exception.InvalidStatusTransitionException;
import com.helpdesk.helpdesk.api.exception.NotFoundException;
import com.helpdesk.helpdesk.api.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TicketService {

    // Data, not if-else chains: the only transitions PATCH /tickets/{id}/status will accept.
    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = Map.of(
            TicketStatus.OPEN, Set.of(TicketStatus.IN_PROGRESS),
            TicketStatus.IN_PROGRESS, Set.of(TicketStatus.RESOLVED),
            TicketStatus.RESOLVED, Set.of(TicketStatus.REOPENED),
            TicketStatus.REOPENED, Set.of(TicketStatus.IN_PROGRESS)
    );

    // Data, not if-else chains: SLA budget per priority, used by the overdue report.
    private static final Map<Priority, Duration> SLA_BY_PRIORITY = Map.of(
            Priority.CRITICAL, Duration.ofHours(4),
            Priority.HIGH, Duration.ofDays(1),
            Priority.MEDIUM, Duration.ofDays(3),
            Priority.LOW, Duration.ofDays(7)
    );

    private final TicketRepository ticketRepository;
    private final UserService userService;
    private final AgentService agentService;

    public TicketService(TicketRepository ticketRepository, UserService userService, AgentService agentService) {
        this.ticketRepository = ticketRepository;
        this.userService = userService;
        this.agentService = agentService;
    }

    @Transactional
    public TicketDetailResponse createTicket(TicketCreateRequest request) {
        User raisedBy = userService.findUserOrThrow(request.getRaisedByUserId());

        Ticket ticket = new Ticket();
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority());
        ticket.setCategory(request.getCategory());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setRaisedBy(raisedBy);

        Ticket saved = ticketRepository.save(ticket);
        return toDetailResponse(saved);
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getTicket(Long id) {
        Ticket ticket = findTicketOrThrow(id);
        return toDetailResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketSummaryResponse> listTickets(TicketStatus status, Priority priority, Category category, Long assignedAgentId) {
        return ticketRepository.search(status, priority, category, assignedAgentId).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional
    public TicketDetailResponse assignAgent(Long ticketId, TicketAssignRequest request) {
        Ticket ticket = findTicketOrThrow(ticketId);
        Agent agent = agentService.findAgentOrThrow(request.getAgentId());

        ticket.setAssignedAgent(agent);

        Ticket saved = ticketRepository.save(ticket);
        return toDetailResponse(saved);
    }

    @Transactional
    public TicketDetailResponse updateStatus(Long ticketId, TicketStatusUpdateRequest request) {
        Ticket ticket = findTicketOrThrow(ticketId);
        TicketStatus current = ticket.getStatus();
        TicketStatus target = request.getStatus();

        Set<TicketStatus> allowedNext = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowedNext.contains(target)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition from " + current + " to " + target);
        }

        ticket.setStatus(target);
        if (target == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if (target == TicketStatus.REOPENED) {
            ticket.setResolvedAt(null);
        }

        Ticket saved = ticketRepository.save(ticket);
        return toDetailResponse(saved);
    }

    @Transactional
    public TicketDetailResponse addComment(Long ticketId, CommentCreateRequest request) {
        Ticket ticket = findTicketOrThrow(ticketId);

        if (request.getAuthorType() == AuthorType.USER) {
            userService.findUserOrThrow(request.getAuthorId());
        } else {
            agentService.findAgentOrThrow(request.getAuthorId());
        }

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthorType(request.getAuthorType());
        comment.setAuthorId(request.getAuthorId());
        comment.setMessage(request.getMessage());
        ticket.getComments().add(comment);

        Ticket saved = ticketRepository.save(ticket);
        return toDetailResponse(saved);
    }

    @Transactional(readOnly = true)
    public AverageResolutionTimeResponse getAverageResolutionTime(Long assignedAgentId, Category category) {
        List<Ticket> resolvedTickets = ticketRepository.findResolvedTickets(assignedAgentId, category);

        if (resolvedTickets.isEmpty()) {
            return new AverageResolutionTimeResponse(0, null);
        }

        double averageHours = resolvedTickets.stream()
                .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMinutes())
                .average()
                .orElse(0) / 60.0;

        return new AverageResolutionTimeResponse(resolvedTickets.size(), averageHours);
    }

    @Transactional(readOnly = true)
    public List<OverdueTicketResponse> getOverdueTickets() {
        LocalDateTime now = LocalDateTime.now();
        return ticketRepository.findByStatusNot(TicketStatus.RESOLVED).stream()
                .filter(t -> now.isAfter(t.getCreatedAt().plus(SLA_BY_PRIORITY.get(t.getPriority()))))
                .map(t -> toOverdueResponse(t, now))
                .toList();
    }

    public Ticket findTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket with id " + id + " not found"));
    }

    private String resolveAuthorName(AuthorType authorType, Long authorId) {
        try {
            if (authorType == AuthorType.USER) {
                return userService.getUser(authorId).getName();
            }
            return agentService.getAgent(authorId).getName();
        } catch (NotFoundException ex) {
            return "Unknown";
        }
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthorType(),
                comment.getAuthorId(),
                resolveAuthorName(comment.getAuthorType(), comment.getAuthorId()),
                comment.getMessage(),
                comment.getCreatedAt());
    }

    private TicketSummaryResponse toSummaryResponse(Ticket ticket) {
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getPriority(),
                ticket.getCategory(),
                ticket.getStatus(),
                ticket.getRaisedBy().getName(),
                ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getName() : null,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }

    private TicketDetailResponse toDetailResponse(Ticket ticket) {
        TicketDetailResponse response = new TicketDetailResponse();
        response.setId(ticket.getId());
        response.setTitle(ticket.getTitle());
        response.setDescription(ticket.getDescription());
        response.setPriority(ticket.getPriority());
        response.setCategory(ticket.getCategory());
        response.setStatus(ticket.getStatus());
        response.setRaisedByUserId(ticket.getRaisedBy().getId());
        response.setRaisedByName(ticket.getRaisedBy().getName());
        if (ticket.getAssignedAgent() != null) {
            response.setAssignedAgentId(ticket.getAssignedAgent().getId());
            response.setAssignedAgentName(ticket.getAssignedAgent().getName());
        }
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());
        response.setComments(ticket.getComments().stream().map(this::toCommentResponse).toList());
        return response;
    }

    private OverdueTicketResponse toOverdueResponse(Ticket ticket, LocalDateTime now) {
        Duration sla = SLA_BY_PRIORITY.get(ticket.getPriority());
        OverdueTicketResponse response = new OverdueTicketResponse();
        response.setId(ticket.getId());
        response.setTitle(ticket.getTitle());
        response.setPriority(ticket.getPriority());
        response.setCategory(ticket.getCategory());
        response.setStatus(ticket.getStatus());
        response.setAssignedAgentName(ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getName() : null);
        response.setCreatedAt(ticket.getCreatedAt());
        response.setSlaHours(sla.toHours());
        response.setHoursOpen(Duration.between(ticket.getCreatedAt(), now).toHours());
        return response;
    }
}
