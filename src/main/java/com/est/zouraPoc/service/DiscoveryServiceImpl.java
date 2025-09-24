package com.est.zouraPoc.service;

import java.util.Map;

import com.est.zouraPoc.Repository.WorkflowExportRepo;
import com.est.zouraPoc.Repository.WorkflowRepo;
import com.est.zouraPoc.dto.ApiResponseWrapperDTO;
import com.est.zouraPoc.model.Workflow;
import com.est.zouraPoc.model.WorkflowExport;
import com.est.zouraPoc.util.WebClientUtil;
import com.est.zouraPoc.util.ZuoraResponseParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Service
public class DiscoveryServiceImpl implements DiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryServiceImpl.class);

    private final WebClientUtil webClientUtil;
    private final OAuthTokenServiceImpl oAuthTokenServiceImpl;
    private final ZuoraResponseParser responseParser;
    private final WorkflowRepo workflowRepository;
    private final WorkflowExportRepo workflowExportRepository;

    @Autowired
    public DiscoveryServiceImpl(WebClientUtil webClientUtil, OAuthTokenServiceImpl oAuthTokenServiceImpl, ZuoraResponseParser responseParser,
            WorkflowRepo workflowRepository, WorkflowExportRepo workflowExportRepository) {
        this.webClientUtil = webClientUtil;
        this.oAuthTokenServiceImpl = oAuthTokenServiceImpl;
        this.responseParser = responseParser;
        this.workflowRepository = workflowRepository;
        this.workflowExportRepository = workflowExportRepository;
    }

    @Override
    public Mono<ApiResponseWrapperDTO> getWorkflow() {
        return oAuthTokenServiceImpl.getToken()
                .flatMap(token -> webClientUtil.get(
                "/workflows",
                String.class,
                Map.of("Authorization", "Bearer " + token))
                .doOnNext(workflowResponse -> {
                    saveWorkflow(workflowResponse);

                })
                .flatMap(responseParser::GetWorkFlowResponse)
                .doOnError(error -> log.error("Error creating Account: {}", error.getMessage())));
    }

    @Transactional
    public void saveWorkflow(String flow) {

        ObjectMapper objectMapper = new ObjectMapper();

        try {

            JsonNode rootNode = objectMapper.readTree(flow);

            JsonNode dataNode = rootNode.path("data");
            int i = 1;
            for (JsonNode item : dataNode) {
                int id = item.path("id").asInt();
                String name = item.path("name").asText();
                String status = item.path("status").asText();

                // Print the extracted values
                //System.out.println("ID: " + id);
                //System.out.println("Name: " + name);
                //System.out.println("Status: " + status);
                Workflow workflow = Workflow.builder().id(id).name(name).status(status).build();
                if (workflow != null) {

                    workflowRepository.save(workflow);
                    getWorkflowExport(id);
                    i++;
                }
            }
            log.error(i + " Rows Inserted");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Mono<ApiResponseWrapperDTO> getWorkflowExport(int id) {

        System.out.println("inside export");
        String exportUrl = "/workflows/" + id + "/export";
        return oAuthTokenServiceImpl.getToken()
                .flatMap(token -> webClientUtil.get(
                exportUrl,
                String.class,
                Map.of("Authorization", "Bearer " + token))
                .doOnNext(workflowResponse -> saveWorkflowExport(workflowResponse))
                .flatMap(responseParser::GetWorkFlowResponse)
                .doOnError(error -> log.error("Error creating Account: {}", error.getMessage())));
    }

    @Transactional
    public void saveWorkflowExport(String flow) {

        ObjectMapper objectMapper = new ObjectMapper();

        try {

            JsonNode rootNode = objectMapper.readTree(flow);

            JsonNode dataNode = rootNode.path("workflow");

            int i = 1;

            for (JsonNode item : dataNode) {
                int id = item.path("id").asInt();
                String name = item.path("name").asText();
                String parameter = item.path("parameters").asText();
                String status = item.path("status").asText();

                // Print the extracted values
                System.out.println("ID: " + id);
                System.out.println("Name: " + name);
                System.out.println("Parameter: " + parameter);
                System.out.println("Status: " + status);

                WorkflowExport workflow = WorkflowExport.builder().id(id).name(name).parameters(parameter).status(status).build();
                if (workflow != null) {
                    workflowExportRepository.save(workflow);
                    i++;
                }
            }
            log.error(i + " Rows Inserted in ");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Mono<ApiResponseWrapperDTO> exportActiveWorkflows() {
        log.info("Starting export of active workflows");
        return oAuthTokenServiceImpl.getToken()
                .flatMap(token -> webClientUtil.get(
                "/workflows",
                String.class,
                Map.of("Authorization", "Bearer " + token))
                .flatMap(workflowsResponse -> processAndSaveActiveWorkflows(workflowsResponse, token))
                .flatMap(responseParser::GetWorkFlowResponse)
                .doOnError(error -> log.error("Error in exportActiveWorkflows: {}", error.getMessage())));
    }

    public Mono<String> processAndSaveActiveWorkflows(String workflowsResponse, String token) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode rootNode = objectMapper.readTree(workflowsResponse);
            JsonNode dataNode = rootNode.path("data");

            // Convert to Flux for reactive processing
            return Flux.fromIterable(dataNode)
                    .flatMap(workflowItem -> {
                        int workflowId = workflowItem.path("id").asInt();
                        String detailUrl = "/workflows/" + workflowId;

                        // Make reactive call to get workflow details
                        return webClientUtil.get(
                                detailUrl,
                                String.class,
                                Map.of("Authorization", "Bearer " + token)
                        )
                                .flatMap(workflowDetail -> {
                                    try {
                                        JsonNode detailNode = objectMapper.readTree(workflowDetail);
                                        String status = detailNode.path("status").asText();

                                        // Only process if status is "Active"
                                        if ("Active".equalsIgnoreCase(status)) {
                                            String name = detailNode.path("name").asText();

                                            // Save workflow data
                                            return saveWorkflowData(workflowId, name, status)
                                                    .then(getAndSaveWorkflowExport(workflowId, token))
                                                    .then(Mono.just(1)); // Return 1 for saved workflow
                                        } else {
                                            log.debug("Skipped non-active workflow: ID={}, Status={}", workflowId, status);
                                            return Mono.just(0); // Return 0 for skipped workflow
                                        }
                                    } catch (IOException e) {
                                        log.error("Error processing workflow detail for ID: {}", workflowId, e);
                                        return Mono.just(0);
                                    }
                                })
                                .onErrorReturn(0); // Return 0 if there's an error fetching details
                    })
                    .collectList()
                    .map(results -> {
                        int totalProcessed = results.size();
                        int totalSaved = results.stream().mapToInt(Integer::intValue).sum();
                        log.info("Export completed. Processed: {}, Saved active workflows: {}", totalProcessed, totalSaved);
                        return "Successfully exported " + totalSaved + " active workflows out of " + totalProcessed + " total workflows";
                    });

        } catch (IOException e) {
            log.error("Error processing workflows response", e);
            return Mono.error(new RuntimeException("Error processing workflows response", e));
        }
    }

    @Transactional
    public Mono<Void> saveWorkflowData(int workflowId, String name, String status) {
        return Mono.fromRunnable(() -> {
            try {
                Workflow workflow = Workflow.builder()
                        .id(workflowId)
                        .name(name)
                        .status(status)
                        .build();

                workflowRepository.save(workflow);
                log.info("Saved active workflow: ID={}, Name={}", workflowId, name);
            } catch (Exception e) {
                log.error("Error saving workflow: ID={}", workflowId, e);
                throw new RuntimeException("Failed to save workflow", e);
            }
        });
    }

    public Mono<Void> getAndSaveWorkflowExport(int workflowId, String token) {
        String exportUrl = "/workflows/" + workflowId + "/export";
        return webClientUtil.get(
                exportUrl,
                String.class,
                Map.of("Authorization", "Bearer " + token)
        )
                .flatMap(exportResponse -> saveWorkflowExportData(exportResponse))
                .doOnError(error -> log.error("Error fetching workflow export for ID: {}", workflowId, error));
    }

    @Transactional
    public Mono<Void> saveWorkflowExportData(String exportResponse) {
        return Mono.fromRunnable(() -> {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode rootNode = objectMapper.readTree(exportResponse);

                // Log the response structure for debugging
                log.debug("Workflow export response: {}", exportResponse);

                // Try different possible structures for the workflow export response
                JsonNode workflowData = null;

                // Check if it's directly a workflow object
                if (rootNode.has("id") && rootNode.has("name")) {
                    workflowData = rootNode;
                } // Check if it's under "workflow" key (single object)
                else if (rootNode.has("workflow") && !rootNode.path("workflow").isArray()) {
                    workflowData = rootNode.path("workflow");
                } // Check if it's under "workflow" key (array)
                else if (rootNode.has("workflow") && rootNode.path("workflow").isArray()) {
                    JsonNode workflowArray = rootNode.path("workflow");
                    if (workflowArray.size() > 0) {
                        workflowData = workflowArray.get(0);
                    }
                } // Check if it's under "data" key
                else if (rootNode.has("data")) {
                    JsonNode dataNode = rootNode.path("data");
                    if (dataNode.isArray() && dataNode.size() > 0) {
                        workflowData = dataNode.get(0);
                    } else if (!dataNode.isArray()) {
                        workflowData = dataNode;
                    }
                }

                if (workflowData != null && workflowData.has("id")) {
                    int id = workflowData.path("id").asInt();
                    String name = workflowData.path("name").asText();
                    String parameters = workflowData.has("parameters") ? workflowData.path("parameters").toString() : "{}";
                    String status = workflowData.path("status").asText();

                    WorkflowExport workflowExport = WorkflowExport.builder()
                            .id(id)
                            .name(name)
                            .parameters(parameters)
                            .status(status)
                            .build();

                    workflowExportRepository.save(workflowExport);
                    log.info("Saved workflow export: ID={}, Name={}", id, name);
                } else {
                    log.warn("No workflow data found in export response. Response structure: {}", rootNode.fieldNames());
                }
            } catch (IOException e) {
                log.error("Error processing workflow export response", e);
                throw new RuntimeException("Failed to process workflow export", e);
            }
        });
    }
}
