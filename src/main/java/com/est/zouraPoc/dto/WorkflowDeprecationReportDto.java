package com.est.zouraPoc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDeprecationReportDto {
    private int id;
    private String name;
    private String status;
    private List<String> deprecatedObjects;
    private String deprecatedObjectsString; // Comma-separated string for Excel
    
    public String getDeprecatedObjectsAsString() {
        if (deprecatedObjectsString != null) {
            return deprecatedObjectsString;
        }
        return deprecatedObjects != null ? String.join(", ", deprecatedObjects) : "";
    }
}