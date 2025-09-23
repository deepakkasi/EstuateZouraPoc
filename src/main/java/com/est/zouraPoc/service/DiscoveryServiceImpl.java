package com.est.zouraPoc.service;

import java.util.Map;

import com.est.zouraPoc.Repository.WorkflowExportRepo;
import com.est.zouraPoc.Repository.WorkflowRepo;
import com.est.zouraPoc.dto.ApiResponseWrapperDTO;
import com.est.zouraPoc.dto.WorkflowExportDto;
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
    public DiscoveryServiceImpl(WebClientUtil webClientUtil,OAuthTokenServiceImpl oAuthTokenServiceImpl,ZuoraResponseParser responseParser,
    		WorkflowRepo workflowRepository,WorkflowExportRepo workflowExportRepository){
	    this.webClientUtil=webClientUtil;
	    this.oAuthTokenServiceImpl=oAuthTokenServiceImpl;
	    this.responseParser=responseParser;
	    this.workflowRepository=workflowRepository;
	    this.workflowExportRepository=workflowExportRepository;
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
            int i=1; 
            for (JsonNode item : dataNode) {
                int id = item.path("id").asInt(); 
                String name = item.path("name").asText();  
                String status = item.path("status").asText();  
                
                // Print the extracted values
                //System.out.println("ID: " + id);
                //System.out.println("Name: " + name);
                //System.out.println("Status: " + status);
                Workflow workflow=Workflow.builder().id(id).name(name).status(status).build();
        		if(workflow!=null) {
        			
        			workflowRepository.save(workflow);
        			getWorkflowExport(id);
        			i++;
        		}
            }
            log.error(i+" Rows Inserted");

        } catch (IOException e) {
            e.printStackTrace();
        }	
	}

	@Override
	public Mono<ApiResponseWrapperDTO> getWorkflowExport(int id) {
		
		System.out.println("inside export");
		String exportUrl="/workflows/"+id+"/export";
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
           
           int i=1;
           
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
               
               WorkflowExport workflow=WorkflowExport.builder().id(id).name(name).parameters(parameter).status(status).build();
       		if(workflow!=null) {
       			workflowExportRepository.save(workflow);
       			i++;
       		}
        }
           log.error(i+" Rows Inserted in ");

       } catch (IOException e) {
           e.printStackTrace();
       }	
	}

}
