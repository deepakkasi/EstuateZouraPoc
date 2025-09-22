package com.est.zouraPoc.dto;

import lombok.Data;

/**
 * DTO representing a reason for an error or status in Zuora responses.
 */
@Data
public class ApiReasonsDTO {
    /**
     * Error or status code.
     */

    private Integer code;
    /**
     * Message describing the reason.
     */
    private String message;
}