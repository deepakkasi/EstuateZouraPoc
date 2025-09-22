package com.est.zouraPoc.dto;

import lombok.Data;

import java.util.List;

/**
 * DTO representing error details returned from Zuora API.
 */
@Data
public class ApiErrorDTO {
    /**
     * Indicates if the request was successful.
     */
    private boolean success;

    /**
     * Process ID associated with the error.
     */
    private String processId;

    /**
     * List of reasons for the error.
     */

    private List<ApiReasonsDTO> reasons;

    /**
     * Request ID associated with the error.
     */

    private String requestId;
}