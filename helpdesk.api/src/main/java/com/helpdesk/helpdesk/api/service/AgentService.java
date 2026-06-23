package com.helpdesk.helpdesk.api.service;

import com.helpdesk.helpdesk.api.dto.request.AgentCreateRequest;
import com.helpdesk.helpdesk.api.dto.response.AgentResponse;
import com.helpdesk.helpdesk.api.entity.Agent;
import com.helpdesk.helpdesk.api.exception.DuplicateEmailException;
import com.helpdesk.helpdesk.api.exception.NotFoundException;
import com.helpdesk.helpdesk.api.repository.AgentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentService {

    private final AgentRepository agentRepository;

    public AgentService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    public AgentResponse createAgent(AgentCreateRequest request) {
        if (agentRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateEmailException("An agent with email " + request.getEmail() + " already exists");
        }
        Agent agent = new Agent();
        agent.setName(request.getName());
        agent.setEmail(request.getEmail());
        Agent saved = agentRepository.save(agent);
        return toResponse(saved);
    }

    public AgentResponse getAgent(Long id) {
        Agent agent = findAgentOrThrow(id);
        return toResponse(agent);
    }

    public List<AgentResponse> listAgents() {
        return agentRepository.findAll().stream().map(this::toResponse).toList();
    }

    public Agent findAgentOrThrow(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agent with id " + id + " not found"));
    }

    private AgentResponse toResponse(Agent agent) {
        return new AgentResponse(agent.getId(), agent.getName(), agent.getEmail(), agent.getCreatedAt());
    }
}
