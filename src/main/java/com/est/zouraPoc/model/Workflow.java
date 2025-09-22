package com.est.zouraPoc.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="workflow")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Workflow {

	@Id
	private int id;
	private String name;
	private String status;
}
