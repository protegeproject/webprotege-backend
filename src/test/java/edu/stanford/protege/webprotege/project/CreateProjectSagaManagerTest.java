package edu.stanford.protege.webprotege.project;

import edu.stanford.protege.webprotege.common.BlobLocation;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.csv.DocumentId;
import edu.stanford.protege.webprotege.icd.projects.*;
import edu.stanford.protege.webprotege.ipc.CommandExecutor;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import edu.stanford.protege.webprotege.ontology.ProcessUploadedOntologiesRequest;
import edu.stanford.protege.webprotege.ontology.ProcessUploadedOntologiesResponse;
import edu.stanford.protege.webprotege.revision.CreateInitialRevisionHistoryRequest;
import edu.stanford.protege.webprotege.revision.CreateInitialRevisionHistoryResponse;
import edu.stanford.protege.webprotege.storage.MinioFileDownloader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateProjectSagaManagerTest {

    private CreateProjectSagaManager manager;

    @Mock
    private ProjectDetailsManager projectDetailsManager;

    @Mock
    private ProjectBranchManager projectBranchManager;

    @Mock
    private CommandExecutor<ProcessUploadedOntologiesRequest, ProcessUploadedOntologiesResponse> processOntologiesExecutor;

    @Mock
    private CommandExecutor<CreateInitialRevisionHistoryRequest, CreateInitialRevisionHistoryResponse> createInitialRevisionHistoryExecutor;

    @Mock
    private CommandExecutor<PrepareBackupFilesForUseRequest, PrepareBackupFilesForUseResponse> prepareBinaryFileBackupForUseExecutor;
    @Mock
    private CommandExecutor<CreateProjectSmallFilesRequest, CreateProjectSmallFilesResponse> createProjectSmallFilesExecutor;

    @Mock
    private MinioFileDownloader fileDownloader;

    @Mock
    private RevisionHistoryReplacer revisionHistoryReplacer;

    @Mock
    private ProjectPermissionsInitializer projectPermissionsInitializer;

    private NewProjectSettings newProjectSettings;

    private UserId janeDoe;

    private NewProjectSettings newProjectSettingsWithSources;

    @Mock
    private ProjectDetails projectDetails;

    @Mock
    private ProjectBranch projectBranch;

    private ExecutionContext executionContext;


    @BeforeEach
    void setUp() {
        manager = new CreateProjectSagaManager(projectDetailsManager,
                projectBranchManager, processOntologiesExecutor,
                                               createInitialRevisionHistoryExecutor,
                                               prepareBinaryFileBackupForUseExecutor,
                                               createProjectSmallFilesExecutor,
                                               fileDownloader,
                                               revisionHistoryReplacer,
                                               projectPermissionsInitializer);
        janeDoe = UserId.valueOf("JaneDoe");
        executionContext = new ExecutionContext(janeDoe, "");
        newProjectSettings = NewProjectSettings.get(janeDoe,
                                                    "TheProjectDisplayName",
                                                    "en",
                                                    "TheProjectDescription");

        var abc = new DocumentId("abc");
        newProjectSettingsWithSources = NewProjectSettings.get(janeDoe,
                                                               "TheProjectDisplayName",
                                                               "en",
                                                               "TheProjectDescription", abc);

    }

    @Test
    void shouldCreateAndRegisterNewEmptyProject() throws InterruptedException {
        manager.execute(newProjectSettings, executionContext);
        Thread.sleep(300);

        verify(projectDetailsManager, times(1)).registerProject(any(ProjectId.class),
                                                                eq(newProjectSettings));
    }

    @Test
    void shouldCreateAndInitPermissionsOnNewEmptyProject() throws InterruptedException {
        var newProjectSettings = NewProjectSettings.get(janeDoe,
                                                        "TheProjectDisplayName",
                                                        "en",
                                                        "TheProjectDescription");
        manager.execute(newProjectSettings, executionContext);
        Thread.sleep(600);
        verify(projectPermissionsInitializer, times(1)).applyDefaultPermissions(any(ProjectId.class),
                                                                                eq(janeDoe));
    }

    @Test
    void shouldHandleCreateNewProjectWithRegisterProjectFailure() throws InterruptedException {
        var errorForTest = new RuntimeException("Error for test");

        doThrow(errorForTest)
                .when(projectDetailsManager).registerProject(any(ProjectId.class), eq(newProjectSettings));
        var result = manager.execute(newProjectSettings, executionContext);
        try {
            result.get();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(ProjectCreationException.class);
            assertThat(e.getCause().getCause()).isEqualTo(errorForTest);
        }
    }

    @Test
    void shouldHandleCreateNewProjectWithInitPermissionsFailure() throws InterruptedException {
        var errorForTest = new RuntimeException("Error for test");
        doThrow(errorForTest)
                .when(projectPermissionsInitializer).applyDefaultPermissions(any(ProjectId.class), eq(janeDoe));
        var result = manager.execute(newProjectSettings, executionContext);
        try {
            result.get();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(ProjectCreationException.class);
            assertThat(e.getCause().getCause()).isEqualTo(errorForTest);
        }
    }

    @Test
    void shouldCreateProjectWithSources() throws ExecutionException, InterruptedException {
        var revisionHistoryLocation = mockCalls();

        var response = manager.execute(newProjectSettingsWithSources, executionContext);
        var r = response.get();

        assertThat(r.projectDetails()).isEqualTo(projectDetails);

        verify(projectDetailsManager, times(1)).registerProject(any(ProjectId.class), eq(newProjectSettingsWithSources));
        verify(projectPermissionsInitializer, times(1)).applyDefaultPermissions(any(ProjectId.class), eq(janeDoe));
        verify(processOntologiesExecutor, times(1)).execute(any(ProcessUploadedOntologiesRequest.class), eq(executionContext));
        verify(createInitialRevisionHistoryExecutor, times(1)).execute(any(CreateInitialRevisionHistoryRequest.class), eq(executionContext));
        verify(fileDownloader, times(1)).downloadFile(eq(revisionHistoryLocation));
    }

    @Test
    void shouldHandleCreateProjectWithSourcesWithOntologyProcessingError() throws InterruptedException {
        when(processOntologiesExecutor.execute(any(ProcessUploadedOntologiesRequest.class), eq(executionContext)))
                .thenReturn(CompletableFuture.failedFuture(new OWLOntologyCreationException()));

        try {
            var response = manager.execute(newProjectSettingsWithSources, executionContext);
            var r = response.get();
            fail("Expected ExecutionException");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(ProjectCreationException.class);
            assertThat(e.getCause().getCause()).isInstanceOf(OWLOntologyCreationException.class);
        }


        verify(projectDetailsManager, never()).registerProject(any(ProjectId.class), eq(newProjectSettingsWithSources));
        verify(projectPermissionsInitializer, never()).applyDefaultPermissions(any(ProjectId.class), eq(janeDoe));
        verify(processOntologiesExecutor, times(1)).execute(any(ProcessUploadedOntologiesRequest.class), eq(executionContext));
        verify(createInitialRevisionHistoryExecutor, never()).execute(any(CreateInitialRevisionHistoryRequest.class), eq(executionContext));
        verify(fileDownloader, never()).downloadFile(any());
    }

    @Test
    void shouldHandleCreateProjectWithSourcesWithRevisionHistoryProcessingError() throws InterruptedException {

        // Mock first stage
        var ontologyLocation = new BlobLocation("testbucket", "testdoc");
        when(processOntologiesExecutor.execute(any(ProcessUploadedOntologiesRequest.class), any(ExecutionContext.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> new ProcessUploadedOntologiesResponse(List.of(ontologyLocation))));


        var errorForTest = new RuntimeException("Error for test");
        when(createInitialRevisionHistoryExecutor.execute(any(CreateInitialRevisionHistoryRequest.class), eq(executionContext)))
                .thenReturn(CompletableFuture.failedFuture(errorForTest));

        try {
            var response = manager.execute(newProjectSettingsWithSources, executionContext);
            var r = response.get();
            fail("Expected ExecutionException");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(ProjectCreationException.class);
            assertThat(e.getCause().getCause()).isEqualTo(errorForTest);
        }


        verify(processOntologiesExecutor, times(1)).execute(any(ProcessUploadedOntologiesRequest.class), eq(executionContext));
        verify(createInitialRevisionHistoryExecutor, times(1)).execute(any(CreateInitialRevisionHistoryRequest.class), eq(executionContext));
        verify(fileDownloader, never()).downloadFile(any());
        verify(projectDetailsManager, never()).registerProject(any(ProjectId.class), eq(newProjectSettingsWithSources));
        verify(projectPermissionsInitializer, never()).applyDefaultPermissions(any(ProjectId.class), eq(janeDoe));
    }

    @Nonnull
    private BlobLocation mockCalls() {
        var ontologyLocation = new BlobLocation("testbucket", "testdoc");
        when(processOntologiesExecutor.execute(any(ProcessUploadedOntologiesRequest.class), any(ExecutionContext.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> new ProcessUploadedOntologiesResponse(List.of(ontologyLocation))));
        var revisionHistoryLocation = new BlobLocation("testbucket", "revisionhistory");
        when(createInitialRevisionHistoryExecutor.execute(any(CreateInitialRevisionHistoryRequest.class), any(ExecutionContext.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> new CreateInitialRevisionHistoryResponse(revisionHistoryLocation)));
        var revisionHistoryPath = Path.of("/tmp/file");
        when(fileDownloader.downloadFile(revisionHistoryLocation))
                .thenReturn(CompletableFuture.supplyAsync(() -> revisionHistoryPath));
        when(revisionHistoryReplacer.replaceRevisionHistory(any(ProjectId.class), eq(revisionHistoryPath)))
                .thenReturn(CompletableFuture.supplyAsync(() -> null));
        doAnswer(inv -> projectDetails)
                .when(projectDetailsManager).getProjectDetails(any(ProjectId.class));
        when(projectPermissionsInitializer.applyDefaultPermissions(any(ProjectId.class), any(UserId.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> null));
        return revisionHistoryLocation;
    }


    @Test
    void shouldCreateProjectFromBackupFile() throws ExecutionException, InterruptedException {
        var revisionHistoryLocation = mockPrepareBackupFileAndDownload();

        when(projectPermissionsInitializer.applyDefaultPermissions(any(ProjectId.class), any(UserId.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        when(createProjectSmallFilesExecutor.execute(any(), eq(executionContext)))
                .thenReturn(CompletableFuture.completedFuture(null));

        when(projectDetailsManager.getProjectDetails(any(ProjectId.class)))
                .thenReturn(projectDetails);

        var response = manager.executeFromBackup(newProjectSettingsWithSources, "someBranch", executionContext);
        var result = response.get();

        assertNotNull(result);
        assertEquals(projectDetails, result.projectDetails());

        verify(prepareBinaryFileBackupForUseExecutor, times(1)).execute(any(), eq(executionContext));
        verify(fileDownloader, times(1)).downloadFile(eq(revisionHistoryLocation));
        verify(revisionHistoryReplacer, times(1)).replaceRevisionHistory(any(ProjectId.class), any(Path.class));
        verify(projectDetailsManager, times(1)).registerProject(any(ProjectId.class), eq(newProjectSettingsWithSources));
        verify(projectBranchManager, times(1)).registerBranchMapping(any(ProjectId.class), eq("someBranch"));
        verify(projectPermissionsInitializer, times(1)).applyDefaultPermissions(any(ProjectId.class), eq(janeDoe));
    }

    @Test
    void shouldHandleErrorDuringBackupFilePreparation() throws InterruptedException {
        var errorForTest = new RuntimeException("Error during backup preparation");
        when(prepareBinaryFileBackupForUseExecutor.execute(any(), eq(executionContext)))
                .thenReturn(CompletableFuture.failedFuture(errorForTest));

        try {
            var response = manager.executeFromBackup(newProjectSettingsWithSources, "someBranch", executionContext);
            response.get(); // This should throw an exception
            fail("Expected ExecutionException");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(ProjectCreationException.class);
            assertThat(e.getCause().getCause()).isEqualTo(errorForTest);
        }

        verify(fileDownloader, never()).downloadFile(any());
        verify(revisionHistoryReplacer, never()).replaceRevisionHistory(any(ProjectId.class), any(Path.class));
        verify(projectDetailsManager, never()).registerProject(any(ProjectId.class), eq(newProjectSettingsWithSources));
        verify(projectBranchManager, never()).registerBranchMapping(any(ProjectId.class), eq("someBranch"));
        verify(projectPermissionsInitializer, never()).applyDefaultPermissions(any(ProjectId.class), eq(janeDoe));
    }


    @Nonnull
    private BlobLocation mockPrepareBackupFileAndDownload() {
        var revisionHistoryLocation = new BlobLocation("testbucket", "backup-history");
        when(prepareBinaryFileBackupForUseExecutor.execute(any(), eq(executionContext)))
                .thenReturn(CompletableFuture.completedFuture(new PrepareBackupFilesForUseResponse(revisionHistoryLocation)));
        var revisionHistoryPath = Path.of("/tmp/revision-history");
        when(fileDownloader.downloadFile(revisionHistoryLocation))
                .thenReturn(CompletableFuture.completedFuture(revisionHistoryPath));
        when(revisionHistoryReplacer.replaceRevisionHistory(any(ProjectId.class), eq(revisionHistoryPath)))
                .thenReturn(CompletableFuture.completedFuture(null));
        return revisionHistoryLocation;
    }
}