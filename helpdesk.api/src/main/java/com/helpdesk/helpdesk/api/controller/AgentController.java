package com.helpdesk.helpdesk.api.controller;

import com.helpdesk.helpdesk.api.dto.request.AgentCreateRequest;
import com.helpdesk.helpdesk.api.dto.response.AgentResponse;
import com.helpdesk.helpdesk.api.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public ResponseEntity<AgentResponse> createAgent(@Valid @RequestBody AgentCreateRequest request) {
        AgentResponse response = agentService.createAgent(request);
        return ResponseEntity.created(URI.create("/api/agents/" + response.getId())).body(response);
    }

    @GetMapping
    public List<AgentResponse> listAgents() {
        return agentService.listAgents();
    }

    @GetMapping("/{id}")
    public AgentResponse getAgent(@PathVariable Long id) {
        return agentService.getAgent(id);
    }
}
