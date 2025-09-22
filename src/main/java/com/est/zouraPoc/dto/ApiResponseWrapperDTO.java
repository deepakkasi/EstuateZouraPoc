package com.est.zouraPoc.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiResponseWrapperDTO {
    private String message;
    private Integer statusCode;
    private Object data;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ApiResponseWrapperDTO(String message, Integer statusCode, Object data) {
        this.message = message;
        this.statusCode = statusCode;
        this.data = data;
    }
}