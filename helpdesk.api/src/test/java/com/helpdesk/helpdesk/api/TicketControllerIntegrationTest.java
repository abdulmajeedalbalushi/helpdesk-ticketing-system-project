package com.helpdesk.helpdesk.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.helpdesk.api.dto.request.AgentCreateRequest;
import com.helpdesk.helpdesk.api.dto.request.CommentCreateRequest;
import com.helpdesk.helpdesk.api.dto.request.TicketAssignRequest;
import com.helpdesk.helpdesk.api.dto.request.TicketCreateRequest;
import com.helpdesk.helpdesk.api.dto.request.TicketStatusUpdateRequest;
import com.helpdesk.helpdesk.api.dto.request.UserCreateRequest;
import com.helpdesk.helpdesk.api.entity.enums.AuthorType;
import com.helpdesk.helpdesk.api.entity.enums.Category;
import com.helpdesk.helpdesk.api.entity.enums.Priority;
import com.helpdesk.helpdesk.api.entity.enums.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullTicketLifecycle() throws Exception {
        UserCreateRequest userRequest = new UserCreateRequest();
        userRequest.setName("Alice Employee");
        userRequest.setEmail("alice@example.com");
        String userJson = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long userId = objectMapper.readTree(userJson).get("id").asLong();

        AgentCreateRequest agentRequest = new AgentCreateRequest();
        agentRequest.setName("Bob Agent");
        agentRequest.setEmail("bob@example.com");
        String agentJson = mockMvc.perform(post("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long agentId = objectMapper.readTree(agentJson).get("id").asLong();

        TicketCreateRequest ticketRequest = new TicketCreateRequest();
        ticketRequest.setTitle("Cannot access VPN");
        ticketRequest.setDescription("VPN client fails to connect from home network.");
        ticketRequest.setPriority(Priority.HIGH);
        ticketRequest.setCategory(Category.NETWORK_SUPPORT);
        ticketRequest.setRaisedByUserId(userId);
        String ticketJson = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ticketRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andReturn().getResponse().getContentAsString();
        Long ticketId = objectMapper.readTree(ticketJson).get("id").asLong();

        // Assigning an agent no longer mutates status - it's independent of the status state machine.
        TicketAssignRequest assignRequest = new TicketAssignRequest();
        assignRequest.setAgentId(agentId);
        mockMvc.perform(post("/api/tickets/" + ticketId + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.assignedAgentName", is("Bob Agent")));

        // Illegal transition: OPEN can only go to IN_PROGRESS, not straight to RESOLVED.
        TicketStatusUpdateRequest illegalSkip = new TicketStatusUpdateRequest();
        illegalSkip.setStatus(TicketStatus.RESOLVED);
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalSkip)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Cannot transition from OPEN to RESOLVED")));

        TicketStatusUpdateRequest toInProgress = new TicketStatusUpdateRequest();
        toInProgress.setStatus(TicketStatus.IN_PROGRESS);
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toInProgress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        TicketStatusUpdateRequest toResolved = new TicketStatusUpdateRequest();
        toResolved.setStatus(TicketStatus.RESOLVED);
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toResolved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")));

        // Illegal transition: RESOLVED cannot go directly back to IN_PROGRESS, only REOPENED.
        TicketStatusUpdateRequest illegalBackToProgress = new TicketStatusUpdateRequest();
        illegalBackToProgress.setStatus(TicketStatus.IN_PROGRESS);
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalBackToProgress)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Cannot transition from RESOLVED to IN_PROGRESS")));

        TicketStatusUpdateRequest toReopened = new TicketStatusUpdateRequest();
        toReopened.setStatus(TicketStatus.REOPENED);
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toReopened)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REOPENED")));

        CommentCreateRequest userComment = new CommentCreateRequest();
        userComment.setAuthorType(AuthorType.USER);
        userComment.setAuthorId(userId);
        userComment.setMessage("Still happening after reboot.");
        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userComment)))
                .andExpect(status().isCreated());

        CommentCreateRequest agentComment = new CommentCreateRequest();
        agentComment.setAuthorType(AuthorType.AGENT);
        agentComment.setAuthorId(agentId);
        agentComment.setMessage("Looking into VPN gateway logs now.");
        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentComment)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tickets/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()", is(2)))
                .andExpect(jsonPath("$.comments[0].authorName", is("Alice Employee")))
                .andExpect(jsonPath("$.comments[1].authorName", is("Bob Agent")));

        // Search filters: combine status + category + assignedAgentId.
        mockMvc.perform(get("/api/tickets")
                        .param("status", "REOPENED")
                        .param("category", "NETWORK_SUPPORT")
                        .param("assignedAgentId", agentId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].id", is(ticketId.intValue())));

        mockMvc.perform(get("/api/tickets").param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));

        // Average resolution time report: this ticket was resolved once, then reopened (resolvedAt cleared),
        // so it should no longer count toward the average.
        mockMvc.perform(get("/api/tickets/reports/avg-resolution-time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedTicketCount", is(0)));

        // Overdue report: a fresh HIGH priority ticket (SLA 1 day) is not yet overdue.
        mockMvc.perform(get("/api/tickets/reports/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }
}
