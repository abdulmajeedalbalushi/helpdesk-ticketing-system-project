package com.helpdesk.helpdesk.api.dto.response;

import com.helpdesk.helpdesk.api.entity.enums.AuthorType;

import java.time.LocalDateTime;

public class CommentResponse {

    private Long id;
    private AuthorType authorType;
    private Long authorId;
    private String authorName;
    private String message;
    private LocalDateTime createdAt;

    public CommentResponse() {
    }

    public CommentResponse(Long id, AuthorType authorType, Long authorId, String authorName,
                            String message, LocalDateTime createdAt) {
        this.id = id;
        this.authorType = authorType;
        this.authorId = authorId;
        this.authorName = authorName;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuthorType getAuthorType() {
        return authorType;
    }

    public void setAuthorType(AuthorType authorType) {
        this.authorType = authorType;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
