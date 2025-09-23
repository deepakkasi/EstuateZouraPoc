package com.est.zouraPoc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="workflowExport")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExport {

	@Id
	@JsonProperty("id")
	private int id;
	@JsonProperty("name")	
	private String name;
	@JsonProperty("parameters")
	private String parameters;
	@JsonProperty("status")
	private String status;
}
