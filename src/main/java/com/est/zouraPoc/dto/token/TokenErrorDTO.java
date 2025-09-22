package com.est.zouraPoc.dto.token;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for standard error response
 */
public record TokenErrorDTO(@JsonProperty("error") String error,
                            @JsonProperty("error_description") String errorDescription) {

}
