package com.est.zouraPoc.util;

import com.est.zouraPoc.dto.token.OAuthTokenResponseDTO;
import com.est.zouraPoc.dto.token.TokenErrorDTO;
import com.est.zouraPoc.exception.TokenGenerationException;
import com.est.zouraPoc.exception.TokenParsingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Reactive utility for OAuth token generation using WebClient.
 * This utility only generates tokens - caching is handled by OAuthTokenServiceImpl
 */
@Component
public class OAuthTokenUtil {

    private static final Logger logger = LoggerFactory.getLogger(OAuthTokenUtil.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClientUtil webClientUtil;

    @Value("${zuora.oauth-token}")
    private String oauthTokenUrl;

    /**
     * Constructor injection ensures dependencies are immutable and testable
     */
    public OAuthTokenUtil(WebClientUtil webClientUtil) {
        this.webClientUtil = webClientUtil;
    }

    /**
     * Generates OAuth token using client credentials reactively.
     * This method ONLY generates the token - it does NOT cache it
     * Caching is handled by OAuthTokenServiceImpl
     */
    public Mono<OAuthTokenResponseDTO> generateToken(String clientId, String clientSecret) {
        if (clientId == null || clientId.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Client ID cannot be null or empty"));
        }
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Client secret cannot be null or empty"));
        }

        logger.info("Starting OAuth token generation for clientId: {}", clientId);

        return Mono.fromCallable(() -> buildRequestBody(clientId, clientSecret))
                .doOnNext(requestBody -> logger.debug("Request body built for OAuth token generation"))
                .flatMap(requestBody -> webClientUtil
                        .request(oauthTokenUrl, HttpMethod.POST, MediaType.APPLICATION_FORM_URLENCODED, requestBody, String.class, null)
                        .doOnNext(response -> logger.info("Received OAuth response from server: {}", clientId))
                        .flatMap(this::parseOAuthResponse)
                        .doOnSuccess(token -> logger.info("OAuth token successfully generated for clientId: {}", clientId))
                        .onErrorMap(ex -> {
                            if (ex instanceof TokenGenerationException || ex instanceof TokenParsingException) {
                                return ex;
                            }
                            logger.error("Error generating OAuth token for clientId: {} - {}", clientId, ex.getMessage(), ex);
                            return new TokenGenerationException("Failed to generate OAuth token: " + ex.getMessage(), ex);
                        }));
    }

    /**
     * Build the form-encoded request body for OAuth token request
     */
    private MultiValueMap<String, String> buildRequestBody(String clientId, String clientSecret) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);

        logger.debug("Built OAuth request body with grant_type=client_credentials, clientId={}", formData);
        return formData;
    }

    /**
     * Extract access_token from OAuth response JSON
     */
    private Mono<OAuthTokenResponseDTO> parseOAuthResponse(String responseBody) {
        return Mono.fromCallable(() -> {
            if (responseBody == null || responseBody.trim().isEmpty()) {
                logger.error("Empty response body received from OAuth server");
                throw new TokenParsingException("Empty response body");
            }

            try {
                if (responseBody.contains("error")) {
                    TokenErrorDTO error = objectMapper.readValue(responseBody, TokenErrorDTO.class);
                    logger.error("OAuth error: {} - {}", error.error(), error.errorDescription());
                    throw new TokenGenerationException("OAuth error: " + error.errorDescription());
                }

                OAuthTokenResponseDTO tokenResponse =
                        objectMapper.readValue(responseBody, OAuthTokenResponseDTO.class);

                if (tokenResponse.accessToken() == null || tokenResponse.accessToken().trim().isEmpty()) {
                    logger.error("Access token is missing or empty in OAuth response");
                    throw new TokenParsingException("Access token is missing in response");
                }

                logger.info("Successfully parsed OAuth response (accessToken length: {})",
                        tokenResponse.accessToken().length());
                return tokenResponse;

            } catch (TokenParsingException parsingError) {
                throw parsingError;
            } catch (Exception e) {
                logger.error("Failed to parse OAuth response JSON: {}", e.getMessage(), e);
                throw new TokenParsingException("Failed to parse OAuth response", e);
            }
        });
    }
}
