package com.est.zouraPoc.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.est.zouraPoc.model.Workflow;



@Repository
public interface WorkflowRepo extends JpaRepository<Workflow, Integer> {

}
