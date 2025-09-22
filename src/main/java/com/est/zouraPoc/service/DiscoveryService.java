package com.est.zouraPoc.service;



import com.est.zouraPoc.dto.ApiResponseWrapperDTO;

import reactor.core.publisher.Mono;

public interface DiscoveryService {
	public Mono<ApiResponseWrapperDTO> getWorkflow();

}
