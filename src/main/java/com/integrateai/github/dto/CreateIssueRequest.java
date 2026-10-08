package com.integrateai.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record CreateIssueRequest(
        @NotBlank(message = "Repository owner is required")
        @JsonProperty("owner") String owner,

        @NotBlank(message = "Repository name is required")
        @JsonProperty("repo") String repo,

        @NotBlank(message = "Issue title is required")
        @JsonProperty("title") String title,

        @JsonProperty("body") String body
) {}
