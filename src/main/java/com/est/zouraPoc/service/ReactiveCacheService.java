package com.est.zouraPoc.service;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

public interface ReactiveCacheService {

    Mono<String> getOrCompute(String key, Supplier<Mono<String>> valueSupplier, Duration expiry);

    Mono<Boolean> setString(String key, String value, Duration expiry);

    Mono<String> getString(String key);

    <T> Mono<T> getOrComputeObject(String key, Supplier<Mono<T>> valueSupplier,
                                   Class<T> type, Duration expiry);

    <T> Mono<Boolean> setObject(String key, T value, Duration expiry);

    <T> Mono<T> getObject(String key, Class<T> type);

    Mono<Boolean> exists(String key);

    Mono<Boolean> delete(String key);

    Mono<Boolean> expire(String key, Duration expiry);

    Mono<Duration> getTTL(String key);

    Mono<Long> deleteKeys(String... keys);

    Mono<Long> deleteByPattern(String pattern);
}
