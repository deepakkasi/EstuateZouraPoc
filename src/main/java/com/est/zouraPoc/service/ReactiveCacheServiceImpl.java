package com.est.zouraPoc.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

@Component
public class ReactiveCacheServiceImpl implements ReactiveCacheService {

    private static final Logger log = LoggerFactory.getLogger(ReactiveCacheServiceImpl.class);

    private final ReactiveRedisTemplate<String, String> stringRedisTemplate;
    private final ReactiveRedisTemplate<String, Object> objectRedisTemplate;

    public ReactiveCacheServiceImpl(ReactiveRedisTemplate<String, String> stringRedisTemplate,
                                    @Qualifier("reactiveObjectRedisTemplate") ReactiveRedisTemplate<String, Object> objectRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectRedisTemplate = objectRedisTemplate;
    }

    // ===== STRING OPERATIONS =====

    /**
     * Get string from cache or compute and cache if not present
     */
    public Mono<String> getOrCompute(String key, Supplier<Mono<String>> valueSupplier, Duration expiry) {
        return stringRedisTemplate.opsForValue().get(key)
                .doOnNext(cachedValue -> log.debug("Cache HIT for key: {}", key))
                .switchIfEmpty(
                        valueSupplier.get()
                                .flatMap(value ->
                                        stringRedisTemplate.opsForValue()
                                                .set(key, value, expiry)
                                                .doOnSuccess(result -> log.debug("Cache SET for key: {}, expiry: {}", key, expiry))
                                                .thenReturn(value)
                                )
                                .doOnNext(value -> log.debug("Cache MISS for key: {}, computed and cached", key))
                );
    }

    /**
     * Set string value in cache
     */
    public Mono<Boolean> setString(String key, String value, Duration expiry) {
        return stringRedisTemplate.opsForValue()
                .set(key, value, expiry)
                .doOnSuccess(result -> log.debug("Cached string for key: {}, expiry: {}", key, expiry));
    }

    /**
     * Get string from cache
     */
    public Mono<String> getString(String key) {
        return stringRedisTemplate.opsForValue().get(key)
                .doOnNext(value -> log.debug("Retrieved string for key: {}", key));
    }

    // ===== OBJECT OPERATIONS =====

    /**
     * Get object from cache or compute and cache if not present
     */
    public <T> Mono<T> getOrComputeObject(String key, Supplier<Mono<T>> valueSupplier,
                                          Class<T> type, Duration expiry) {
        return objectRedisTemplate.opsForValue().get(key)
                .cast(type)
                .doOnNext(cachedValue -> log.debug("Cache HIT for object key: {}", key))
                .switchIfEmpty(
                        valueSupplier.get()
                                .flatMap(value ->
                                        objectRedisTemplate.opsForValue()
                                                .set(key, value, expiry)
                                                .doOnSuccess(result -> log.debug("Cache SET for object key: {}, expiry: {}", key, expiry))
                                                .thenReturn(value)
                                )
                                .doOnNext(value -> log.debug("Cache MISS for object key: {}, computed and cached", key))
                );
    }

    /**
     * Set object in cache
     */
    public <T> Mono<Boolean> setObject(String key, T value, Duration expiry) {
        return objectRedisTemplate.opsForValue()
                .set(key, value, expiry)
                .doOnSuccess(result -> log.debug("Cached object for key: {}, expiry: {}", key, expiry));
    }

    /**
     * Get object from cache
     */
    public <T> Mono<T> getObject(String key, Class<T> type) {
        return objectRedisTemplate.opsForValue().get(key)
                .cast(type)
                .doOnNext(value -> log.debug("Retrieved object for key: {}", key));
    }

    // ===== COMMON OPERATIONS =====

    /**
     * Check if key exists
     */
    public Mono<Boolean> exists(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * Delete key from cache
     */
    public Mono<Boolean> delete(String key) {
        return stringRedisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnNext(deleted -> {
                    if (Boolean.TRUE.equals(deleted)) {
                        log.debug("Deleted key: {}", key);
                    } else {
                        log.debug("Key not found for deletion: {}", key);
                    }
                });
    }

    /**
     * Set expiry for existing key
     */
    public Mono<Boolean> expire(String key, Duration expiry) {
        return stringRedisTemplate.expire(key, expiry)
                .doOnNext(result -> log.debug("Set expiry for key: {}, duration: {}, success: {}", key, expiry, result));
    }

    /**
     * Get TTL for key
     */
    public Mono<Duration> getTTL(String key) {
        return stringRedisTemplate.getExpire(key)
                .doOnNext(ttl -> log.debug("TTL for key: {} is: {}", key, ttl));
    }

    /**
     * Delete multiple keys
     */
    public Mono<Long> deleteKeys(String... keys) {
        return stringRedisTemplate.delete(keys)
                .doOnNext(count -> log.debug("Deleted {} keys", count));
    }

    /**
     * Delete keys matching pattern
     */
    public Mono<Long> deleteByPattern(String pattern) {
        return stringRedisTemplate.scan(ScanOptions.scanOptions().match(pattern).build())
                .collectList()
                .flatMap(keyList -> {
                    if (keyList.isEmpty()) {
                        return Mono.just(0L);
                    }
                    return stringRedisTemplate.delete(keyList.toArray(new String[0]));
                })
                .doOnNext(count -> log.debug("Deleted {} keys matching pattern: {}", count, pattern));
    }
}