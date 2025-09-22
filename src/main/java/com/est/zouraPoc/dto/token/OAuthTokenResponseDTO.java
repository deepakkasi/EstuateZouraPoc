package com.est.zouraPoc.dto.token;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OAuthTokenResponseDTO(@JsonProperty("access_token") String accessToken,
                                    @JsonProperty("token_type") String tokenType,
                                    @JsonProperty("expires_in") int expiresIn,
                                    @JsonProperty("scope") String scope,
                                    @JsonProperty("jti") String jti) {

}
