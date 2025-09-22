package com.est.zouraPoc.service;

import reactor.core.publisher.Mono;

public interface OAuthTokenService {

    Mono<String> getToken();
}
