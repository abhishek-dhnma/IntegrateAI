package com.integrateai.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubIssue(
        @JsonProperty("id") Long id,
        @JsonProperty("number") Integer number,
        @JsonProperty("title") String title,
        @JsonProperty("body") String body,
        @JsonProperty("state") String state,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt,
        @JsonProperty("user") GitHubUser user
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubUser(
            @JsonProperty("login") String login,
            @JsonProperty("avatar_url") String avatarUrl
    ) {}
}
