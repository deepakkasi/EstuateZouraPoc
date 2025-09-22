package com.est.zouraPoc.service;




import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.est.zouraPoc.dto.token.OAuthTokenResponseDTO;
import com.est.zouraPoc.util.OAuthTokenUtil;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * OAuth Token Service with Redis caching
 * Flow: Check Redis → If not found → Generate → Store in Redis → Return
 */
@Service
public class OAuthTokenServiceImpl  implements OAuthTokenService {

    private static final Logger logger = LoggerFactory.getLogger(OAuthTokenServiceImpl.class);
    private static final String TOKEN_CACHE_KEY = "oauth:token:";
    private static final Duration TOKEN_CACHE_DURATION = Duration.ofMinutes(55); // Cache for slightly less than 1 hour

    private final OAuthTokenUtil oAuthTokenUtil;
    private final ReactiveCacheService cacheService;

    @Value("${zuora.clientId}")
    private String clientId;

    @Value("${zuora.clientSecret}")
    private String clientSecret;

    public OAuthTokenServiceImpl(
                                 OAuthTokenUtil oAuthTokenUtil,
                                 ReactiveCacheService cacheService) {
        
        this.oAuthTokenUtil = oAuthTokenUtil;
        this.cacheService = cacheService;
    }


    public Mono<String> getToken() {
        String cacheKey = TOKEN_CACHE_KEY + clientId;

        return cacheService.getOrCompute(
                cacheKey,
                this::generateNewToken,
                TOKEN_CACHE_DURATION
        );
    }

    private Mono<String> generateNewToken() {
        return oAuthTokenUtil.generateToken(clientId, clientSecret)
                .map(OAuthTokenResponseDTO::accessToken)
                .doOnSuccess(token -> logger.info("New token generated and cached for clientId: {}", clientId))
                .doOnError(error -> logger.error("Failed to generate token for clientId: {} - {}",
                        clientId, error.getMessage()));
    }
}
