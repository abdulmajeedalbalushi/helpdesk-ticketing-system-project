package com.helpdesk.helpdesk.api.dto.request;

import com.helpdesk.helpdesk.api.entity.enums.AuthorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CommentCreateRequest {

    @NotNull(message = "authorType is required")
    private AuthorType authorType;

    @NotNull(message = "authorId is required")
    private Long authorId;

    @NotBlank(message = "message is required")
    private String message;

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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
