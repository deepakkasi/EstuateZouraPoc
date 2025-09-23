package com.est.zouraPoc.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.est.zouraPoc.model.Workflow;
import com.est.zouraPoc.model.WorkflowExport;



@Repository
public interface WorkflowExportRepo extends JpaRepository<WorkflowExport, Integer> {

}
