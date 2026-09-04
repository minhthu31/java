package vn.edu.cnpm.projectsupport.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraAdfNodeDto(String type, String text, Map<String, Object> attrs, List<Map<String, Object>> marks, List<JiraAdfNodeDto> content) {
}