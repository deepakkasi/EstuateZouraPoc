package com.est.zouraPoc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class WebClientUtil {

    private static final Logger log = LoggerFactory.getLogger(WebClientUtil.class);
    private final WebClient webClient;

    public WebClientUtil(@Value("${zuora.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    // --- Public API shortcuts ---
    public <R> Mono<R> get(String uri, Class<R> responseType, Map<String, String> headers, Object... uriVariables) {
        return request(uri, HttpMethod.GET, MediaType.APPLICATION_JSON, null, responseType, headers, uriVariables);
    }

    // Public API without auth - for token generation
    public <T, R> Mono<R> post(String uri, T body, Class<R> responseType, Map<String, String> headers) {
        return request(uri, HttpMethod.POST, MediaType.APPLICATION_JSON, body, responseType, headers);
    }

    public <T, R> Mono<R> put(String uri, T body, Class<R> responseType, Map<String, String> headers, Object... uriVariables) {
        return request(uri, HttpMethod.PUT, MediaType.APPLICATION_JSON, body, responseType, headers, uriVariables);
    }

    public Mono<byte[]> getFile(String uri, Map<String, String> headers, Object... uriVariables) {
        return request(uri, HttpMethod.GET, MediaType.APPLICATION_PDF, null, byte[].class, headers, uriVariables);
    }

    // --- Core Request Method ---
    public <T, R> Mono<R> request(
            String uri,
            HttpMethod method,
            MediaType contentType,
            T body,
            Class<R> responseType,
            Map<String, String> headers,
            Object... uriVariables
    ) {
        WebClient.RequestBodySpec requestSpec = webClient.method(method)
                .uri(uriBuilder -> uriBuilder.path(uri).build(uriVariables))
                .contentType(contentType)
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PDF);

        // Apply custom headers dynamically
        if (headers != null) {
            headers.forEach(requestSpec::header);
        }

        WebClient.RequestHeadersSpec<?> spec;

        if (body != null) {
            if (MediaType.APPLICATION_FORM_URLENCODED.equals(contentType) && body instanceof MultiValueMap) {
                @SuppressWarnings("unchecked")
                MultiValueMap<String, String> formData = (MultiValueMap<String, String>) body;
                spec = requestSpec.body(BodyInserters.fromFormData(formData));
            } else {
                spec = requestSpec.bodyValue(body);
            }
        } else {
            spec = requestSpec;
        }

        return spec.exchangeToMono(response -> {
            // raw response available here
            log.debug("Status: {}", response.statusCode());
            log.debug("Headers: {}", response.headers().asHttpHeaders());
            log.debug("Response:{}", response);
            log.debug("Response body: {}", response.bodyToMono(String.class)
                    .doOnNext(responseBody -> log.debug("Response Body: {}", responseBody)));
            if (response.statusCode().isError()) {
                return response.createException()
                        .flatMap(Mono::error);
            }

            return response.bodyToMono(responseType);
        });
    }

}
