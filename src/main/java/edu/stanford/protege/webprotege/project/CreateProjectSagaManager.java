package edu.stanford.protege.webprotege.project;

import edu.stanford.protege.webprotege.common.*;
import edu.stanford.protege.webprotege.icd.projects.*;
import edu.stanford.protege.webprotege.ipc.*;
import edu.stanford.protege.webprotege.ontology.*;
import edu.stanford.protege.webprotege.revision.*;
import edu.stanford.protege.webprotege.storage.MinioFileDownloader;
import org.slf4j.*;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2024-05-07
 */
@Component
public class CreateProjectSagaManager {

    private static final Logger logger = LoggerFactory.getLogger(CreateProjectSagaManager.class);

    private final ProjectDetailsManager projectDetailsManager;

    private final ProjectBranchManager projectBranchManager;

    private final CommandExecutor<ProcessUploadedOntologiesRequest, ProcessUploadedOntologiesResponse> processOntologiesExecutor;

    private final CommandExecutor<CreateInitialRevisionHistoryRequest, CreateInitialRevisionHistoryResponse> createInitialRevisionHistoryExecutor;
    private final CommandExecutor<PrepareBackupFilesForUseRequest, PrepareBackupFilesForUseResponse> prepareBinaryFileBackupForUseExecutor;

    private final CommandExecutor<CreateProjectSmallFilesRequest, CreateProjectSmallFilesResponse> createProjectSmallFilesExecutor;

    private final MinioFileDownloader fileDownloader;

    private final RevisionHistoryReplacer revisionHistoryReplacer;

    private final ProjectPermissionsInitializer projectPermissionsInitializer;


    public CreateProjectSagaManager(ProjectDetailsManager projectDetailsManager,
                                    ProjectBranchManager projectBranchManager, CommandExecutor<ProcessUploadedOntologiesRequest, ProcessUploadedOntologiesResponse> processOntologiesExecutor,
                                    CommandExecutor<CreateInitialRevisionHistoryRequest, CreateInitialRevisionHistoryResponse> createInitialRevisionHistoryExecutor,
                                    CommandExecutor<PrepareBackupFilesForUseRequest, PrepareBackupFilesForUseResponse> prepareBinaryFileBackupForUseExecutor,
                                    CommandExecutor<CreateProjectSmallFilesRequest, CreateProjectSmallFilesResponse> createProjectSmallFilesExecutor,
                                    MinioFileDownloader fileDownloader,
                                    RevisionHistoryReplacer revisionHistoryReplacer,
                                    ProjectPermissionsInitializer projectPermissionsInitializer) {
        this.projectDetailsManager = projectDetailsManager;
        this.projectBranchManager = projectBranchManager;
        this.processOntologiesExecutor = processOntologiesExecutor;
        this.createInitialRevisionHistoryExecutor = createInitialRevisionHistoryExecutor;
        this.prepareBinaryFileBackupForUseExecutor = prepareBinaryFileBackupForUseExecutor;
        this.createProjectSmallFilesExecutor = createProjectSmallFilesExecutor;
        this.revisionHistoryReplacer = revisionHistoryReplacer;
        this.fileDownloader = fileDownloader;
        this.projectPermissionsInitializer = projectPermissionsInitializer;
    }


    public CompletableFuture<CreateNewProjectResult> execute(NewProjectSettings newProjectSettings, ExecutionContext executionContext) {
        if (newProjectSettings.hasSourceDocument()) {
            return createProjectFromSources(new SagaStateWithSources(ProjectId.generate(), newProjectSettings, executionContext));
        } else {
            return createEmptyProject(new SagaState(ProjectId.generate(), newProjectSettings, executionContext));
        }
    }


    private CompletableFuture<CreateNewProjectResult> createEmptyProject(SagaState sagaState) {
        logger.info("Creating an empty project: {}", sagaState.newProjectSettings);
        return registerProject(sagaState).thenCompose(this::initializeProjectPermissions)
                .thenCompose(this::retrieveProjectDetails)
                .handle((r, e) -> {
                    if (e == null) {
                        return new CreateNewProjectResult(r.getProjectDetails());
                    } else {
                        // Should be a CompletionException
                        logger.error("Error creating project", e);
                        throw new ProjectCreationException(sagaState.getProjectId(),
                                "Project creation failed",
                                e.getCause());
                    }
                });
    }

    private CompletableFuture<CreateNewProjectResult> createProjectFromSources(SagaStateWithSources sagaState) {
        logger.info("Creating a project from sources: {}", sagaState.getNewProjectSettings());
        return processSources(sagaState).thenCompose(this::createInitialRevisionHistory)
                .thenCompose(this::downloadRevisionHistory)
                .thenCompose(this::copyRevisionHistoryToProject)
                .thenCompose(this::registerProject)
                .thenCompose(this::initializeProjectPermissions)
                .thenCompose(this::retrieveProjectDetails)
                .handle((r, e) -> {
                    if (e == null) {
                        return new CreateNewProjectResult(r.getProjectDetails());
                    } else {
                        logger.error("Error creating project", e);
                        logger.error("Error creating project", e);
                        throw new ProjectCreationException(sagaState.getProjectId(),
                                "Project creation failed",
                                e.getCause());
                    }
                });
    }

    public CompletableFuture<CreateNewProjectFromProjectBackupResult> executeFromBackup(NewProjectSettings newProjectSettings, String branchName, ExecutionContext executionContext) {
        if (newProjectSettings.hasSourceDocument()) {
            return createProjectFromBackupFile(new SagaStateWithSources(ProjectId.generate(), newProjectSettings, executionContext), branchName);
        }
        return null;
    }

    private CompletableFuture<CreateNewProjectFromProjectBackupResult> createProjectFromBackupFile(SagaStateWithSources sagaState, String branchName) {
        logger.info("Creating an empty project: {}", sagaState.getNewProjectSettings());
        return prepareBackupFilesForRestore(sagaState)
                .thenCompose(this::downloadRevisionHistory)
                .thenCompose(this::copyRevisionHistoryToProject)
                .thenCompose(this::createProjectSmallFiles)
                .thenCompose((sagaStateWithSources) -> this.mapProjectToBranch(sagaStateWithSources, branchName))
                .thenCompose(this::registerProject)
                .thenCompose(this::initializeProjectPermissions)
                .thenCompose(this::retrieveProjectDetails)
                .handle((r, e) -> {
                    if (e == null) {
                        return new CreateNewProjectFromProjectBackupResult(r.getProjectDetails());
                    } else {
                        // Should be a CompletionException
                        logger.error("Error creating project", e);
                        throw new ProjectCreationException(sagaState.getProjectId(),
                                "Project creation failed",
                                e.getCause());
                    }
                });
    }

    private CompletableFuture<SagaStateWithSources> mapProjectToBranch(SagaStateWithSources sagaState, String branchName) {
        return CompletableFuture.supplyAsync(() -> {
            projectBranchManager.registerBranchMapping(sagaState.getProjectId(), branchName);
            return sagaState;
        });
    }

    private CompletableFuture<SagaStateWithSources> createProjectSmallFiles(SagaStateWithSources sagaState) {
        var createHistoryRequest = sagaState.createProjectSmallFiles();
        return createProjectSmallFilesExecutor.execute(createHistoryRequest, sagaState.getExecutionContext())
                .thenApply(response -> sagaState);
    }

    private CompletableFuture<SagaStateWithSources> prepareBackupFilesForRestore(SagaStateWithSources sagaState) {
        var createHistoryRequest = sagaState.createPrepareBackupFilesForUseRequest();
        return prepareBinaryFileBackupForUseExecutor.execute(createHistoryRequest, sagaState.getExecutionContext())
                .thenApply(sagaState::handleBackupFilesReady);
    }

    private CompletableFuture<SagaState> retrieveProjectDetails(SagaState sagaState) {
        return CompletableFuture.supplyAsync(() -> projectDetailsManager.getProjectDetails(sagaState.getProjectId()))
                .thenApply(sagaState::setProjectDetails);
    }

    private CompletableFuture<SagaState> initializeProjectPermissions(SagaState sagaState) {
        var projectOwner = sagaState.getNewProjectSettings().getProjectOwner();
        return projectPermissionsInitializer.applyDefaultPermissions(sagaState.getProjectId(), projectOwner)
                .thenApply(r -> sagaState);
    }

    private CompletableFuture<SagaStateWithSources> processSources(SagaStateWithSources sagaState) {
        var request = sagaState.createProcessUploadedOntologiesRequest();
        return processOntologiesExecutor.execute(request, sagaState.getExecutionContext())
                .thenApply(sagaState::handleProcessUploadedOntologiesResponse);
    }

    private CompletableFuture<SagaStateWithSources> createInitialRevisionHistory(SagaStateWithSources sagaState) {
        var createHistoryRequest = sagaState.createCreateInitialRevisionHistoryRequest();
        return createInitialRevisionHistoryExecutor.execute(createHistoryRequest, sagaState.getExecutionContext())
                .thenApply(sagaState::handleCreateInitialRevisionHistoryResponse);
    }

    private CompletableFuture<SagaStateWithSources> downloadRevisionHistory(SagaStateWithSources sagaState) {
        return fileDownloader.downloadFile(sagaState.getRevisionHistoryLocation()).thenApply(sagaState::setRevisionHistoryPath);
    }

    private CompletableFuture<SagaStateWithSources> copyRevisionHistoryToProject(SagaStateWithSources sagaState) {
        return revisionHistoryReplacer.replaceRevisionHistory(sagaState.getProjectId(),
                        sagaState.getRevisionHistoryPath())
                .thenApply(r -> sagaState);
    }

    private CompletableFuture<SagaState> registerProject(SagaState sagaState) {
        return CompletableFuture.supplyAsync(() -> {
            projectDetailsManager.registerProject(sagaState.getProjectId(), sagaState.getNewProjectSettings());
            return sagaState;
        });
    }


    private static class SagaState {

        private final ProjectId projectId;

        private final NewProjectSettings newProjectSettings;

        private final ExecutionContext executionContext;

        private ProjectDetails projectDetails;

        private SagaState(ProjectId projectId, NewProjectSettings newProjectSettings, ExecutionContext executionContext) {
            this.projectId = Objects.requireNonNull(projectId);
            this.newProjectSettings = newProjectSettings;
            this.executionContext = executionContext;
        }

        public ProjectId getProjectId() {
            return projectId;
        }

        public NewProjectSettings getNewProjectSettings() {
            return newProjectSettings;
        }

        public ExecutionContext getExecutionContext() {
            return executionContext;
        }

        public SagaState setProjectDetails(ProjectDetails projectDetails) {
            this.projectDetails = projectDetails;
            return this;
        }

        public ProjectDetails getProjectDetails() {
            if (projectDetails == null) {
                throw new IllegalStateException("ProjectDetails is null");
            }
            return projectDetails;
        }
    }

    private static class SagaStateWithSources extends SagaState {

        private List<BlobLocation> ontologyDocumentLocations;

        private BlobLocation revisionHistoryLocation;

        private Path revisionHistoryPath;

        public SagaStateWithSources(ProjectId projectId, NewProjectSettings newProjectSettings, ExecutionContext executionContext) {
            super(projectId, newProjectSettings, executionContext);
        }

        public BlobLocation getRevisionHistoryLocation() {
            return Objects.requireNonNull(revisionHistoryLocation);
        }

        public Path getRevisionHistoryPath() {
            return Objects.requireNonNull(revisionHistoryPath);
        }

        public SagaStateWithSources setRevisionHistoryPath(Path revisionHistoryPath) {
            this.revisionHistoryPath = revisionHistoryPath;
            return this;
        }

        public CreateInitialRevisionHistoryRequest createCreateInitialRevisionHistoryRequest() {
            return new CreateInitialRevisionHistoryRequest(ontologyDocumentLocations);
        }

        public SagaStateWithSources handleCreateInitialRevisionHistoryResponse(CreateInitialRevisionHistoryResponse response) {
            this.revisionHistoryLocation = response.revisionHistoryLocation();
            return this;
        }

        public ProcessUploadedOntologiesRequest createProcessUploadedOntologiesRequest() {
            return new ProcessUploadedOntologiesRequest(getNewProjectSettings().sourceDocument());
        }

        public SagaStateWithSources handleProcessUploadedOntologiesResponse(ProcessUploadedOntologiesResponse response) {
            this.ontologyDocumentLocations = response.ontologies();
            return this;
        }

        public SagaStateWithSources handleBackupFilesReady(PrepareBackupFilesForUseResponse response) {
            this.revisionHistoryLocation = response.binaryFileLocation();
            return this;
        }

        public PrepareBackupFilesForUseRequest createPrepareBackupFilesForUseRequest() {
            return new PrepareBackupFilesForUseRequest(getProjectId(), getNewProjectSettings().sourceDocument());
        }

        public CreateProjectSmallFilesRequest createProjectSmallFiles() {
            return new CreateProjectSmallFilesRequest(getProjectId());
        }
    }

}
