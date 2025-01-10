package edu.stanford.protege.webprotege.icd.projects;

import edu.stanford.protege.webprotege.common.ProjectId;
import org.springframework.stereotype.Component;

@Component
public class ProjectBranchManager {

    private final ProjectBranchRepository branchRepository;

    public ProjectBranchManager(ProjectBranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }


    public void registerBranchMapping(ProjectId projectId, String branchName) {
        branchRepository.save(ProjectBranch.get(projectId, branchName));
    }
}
