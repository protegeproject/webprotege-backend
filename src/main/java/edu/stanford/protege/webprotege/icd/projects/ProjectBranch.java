package edu.stanford.protege.webprotege.icd.projects;

import com.fasterxml.jackson.annotation.*;
import com.google.auto.value.AutoValue;
import edu.stanford.protege.webprotege.common.*;
import edu.stanford.protege.webprotege.project.*;

import javax.annotation.*;
import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;


@AutoValue
public abstract class ProjectBranch implements Serializable, Comparable<ProjectBranch>, HasProjectId {


    public static final String PROJECT_ID = "_id";

    public static final String BRANCH_NAME = "branchName";

    public static final String PROJECT_BRANCH_COLLECTION = "ProjectBranch";

    /**
     * Constructs a {@link ProjectBranch} object.
     *
     * @param projectId      The {@link ProjectId} that identifies the project which these details describe.
     * @param branchName    The name for the project branch in whofic-ontology-files repo.  Not {@code null}.
     *
     * @throws NullPointerException if any parameters are {@code null}.
     */
    public static ProjectBranch get(@Nonnull ProjectId projectId,
                                    @Nonnull String branchName) {
        return new AutoValue_ProjectBranch(projectId,
                                            branchName);
    }

    /**
     * Separate factory method for Jackson that deals with default values in case values are not present (are null)
     * and also deals with Instant.
     */
    @JsonCreator
    public static ProjectBranch valueOf(@Nonnull @JsonProperty(PROJECT_ID) ProjectId projectId,
                                        @Nonnull @JsonProperty(BRANCH_NAME) String branchName) {
        return get(projectId,
                branchName);
    }

    public ProjectBranch withBranchName(@Nonnull String branchName) {
        if(branchName.equals(getBranchName())) {
            return this;
        }
        else {
            return get(projectId(),
                       branchName);
        }
    }

    /**
     * Gets the {@link ProjectId} of the project that these details describe.
     *
     * @return The {@link ProjectId}.  Not {@code null}.
     */
    @JsonProperty(PROJECT_ID)
    @Nonnull
    public abstract ProjectId projectId();

    /**
     * Gets the name of the branch for the project in whofic-ontology-files repo.
     *
     * @return The branchName name.  Not {@code null}.
     */
    @JsonProperty(BRANCH_NAME)
    @Nonnull
    public abstract String getBranchName();


    @Override
    public int compareTo(ProjectBranch o) {
        final int branchNameDiff = getBranchName().compareToIgnoreCase(o.getBranchName());
        if (branchNameDiff != 0) {
            return branchNameDiff;
        }

        return projectId().id().compareTo(o.projectId().id());
    }
}
