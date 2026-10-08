package com.integrateai.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepo(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("description") String description,
        @JsonProperty("private") Boolean isPrivate,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("stargazers_count") Integer starsCount,
        @JsonProperty("open_issues_count") Integer openIssuesCount,
        @JsonProperty("updated_at") String updatedAt,
        @JsonProperty("language") String language
) {}
