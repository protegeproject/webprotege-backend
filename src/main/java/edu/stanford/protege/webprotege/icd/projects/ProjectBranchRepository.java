package edu.stanford.protege.webprotege.icd.projects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.inject.ApplicationSingleton;
import edu.stanford.protege.webprotege.persistence.Repository;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.data.mongodb.core.MongoTemplate;

import javax.annotation.*;
import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.locks.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static edu.stanford.protege.webprotege.icd.projects.ProjectBranch.PROJECT_ID;

@ApplicationSingleton
public class ProjectBranchRepository implements Repository {

    @Nonnull
    private final ObjectMapper objectMapper;
    private final MongoCollection<Document> collection;


    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Lock readLock = readWriteLock.readLock();

    private final Lock writeLock = readWriteLock.writeLock();

    @Inject
    public ProjectBranchRepository(@Nonnull MongoTemplate mongoTemplate,
                                   @Nonnull ObjectMapper objectMapper) {
        this.collection = mongoTemplate.getCollection(ProjectBranch.PROJECT_BRANCH_COLLECTION);
        this.objectMapper = checkNotNull(objectMapper);
    }

    @Nullable
    public ProjectBranch findOneFromDbOrNull(@NonNull ProjectId projectId) {
        return findOneFromDb(projectId).orElse(null);
    }

    private Optional<ProjectBranch> findOneFromDb(@Nonnull ProjectId projectId) {
        return Optional
                .ofNullable(collection.find(withProjectId(projectId))
                        .limit(1)
                        .first())
                .map(d -> objectMapper.convertValue(d, ProjectBranch.class));
    }

    private static Document withProjectId(@Nonnull ProjectId projectId) {
        return new Document(PROJECT_ID, projectId.id());
    }

    public boolean containsProject(@Nonnull ProjectId projectId) {
        try {
            readLock.lock();
            return collection.find(withProjectId(projectId)).projection(new Document()).limit(1).first() != null;
        } finally {
            readLock.unlock();
        }
    }

    public void setBranchName(ProjectId projectId,
                              String branchName) {
        try {
            writeLock.lock();
            collection.updateOne(withProjectId(projectId), updateBranchName(branchName));
        } finally {
            writeLock.unlock();
        }
    }

    public static Bson updateBranchName(String branchName) {
        return Updates.set(ProjectBranch.BRANCH_NAME, branchName);
    }

    public void save(@Nonnull ProjectBranch projectBranch) {
        try {
            writeLock.lock();
            var document = objectMapper.convertValue(projectBranch, Document.class);
            var projectId = projectBranch.projectId();
            collection.replaceOne(withProjectId(projectId), document, new ReplaceOptions().upsert(true));
        } finally {
            writeLock.unlock();
        }
    }

    public void delete(@Nonnull ProjectId projectId) {
        try {
            writeLock.lock();
            collection.deleteOne(withProjectId(projectId));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void ensureIndexes() {
        collection.createIndex(new Document(ProjectBranch.PROJECT_ID, 1));
    }
}
