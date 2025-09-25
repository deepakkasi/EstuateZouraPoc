package com.est.zouraPoc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;

import com.est.zouraPoc.dto.ApiResponseWrapperDTO;
import com.est.zouraPoc.service.DiscoveryService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    private final DiscoveryService DiscoveryService;

    @Autowired
    MainController(DiscoveryService DiscoveryService) {
        this.DiscoveryService = DiscoveryService;
    }

    @GetMapping("/discovery")
    public Mono<ApiResponseWrapperDTO> discovery() {
        return DiscoveryService.exportActiveWorkflows();
    }

    @GetMapping("/export-workflows-excel")
    public Mono<ResponseEntity<Resource>> exportWorkflowsToExcel() {
        log.info("Excel export endpoint called");
        return DiscoveryService.exportActiveWorkflowsToExcel()
                .map(resource -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=workflow-deprecation-report.xlsx")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource))
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

}
