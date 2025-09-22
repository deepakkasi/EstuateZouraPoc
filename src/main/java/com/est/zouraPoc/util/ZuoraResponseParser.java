package com.est.zouraPoc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.est.zouraPoc.dto.ApiResponseWrapperDTO;

import reactor.core.publisher.Mono;

@Component
public class ZuoraResponseParser {

    private static final Logger log = LoggerFactory.getLogger(ZuoraResponseParser.class);

    public Mono<ApiResponseWrapperDTO> GetWorkFlowResponse(String responseDTO) {

            log.error("UNABLE_TO_CREATE_ACCOUNT_WITH_SUBSCRIPTION {}", responseDTO);
            ApiResponseWrapperDTO responseWrapperDTO = new ApiResponseWrapperDTO("UNABLE_TO_CREATE_ACCOUNT", ZuoraErrorParser.resolveStatus(200), responseDTO);
            return Mono.just(responseWrapperDTO);
        
    }

}