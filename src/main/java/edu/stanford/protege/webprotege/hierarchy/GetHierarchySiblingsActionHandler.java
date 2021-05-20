package edu.stanford.protege.webprotege.hierarchy;

import edu.stanford.protege.webprotege.access.AccessManager;
import edu.stanford.protege.webprotege.dispatch.AbstractProjectActionHandler;
import edu.stanford.protege.webprotege.dispatch.ExecutionContext;
import edu.stanford.protege.webprotege.pagination.PageCollector;
import edu.stanford.protege.webprotege.shortform.DictionaryManager;
import edu.stanford.protege.webprotege.access.BuiltInAction;
import edu.stanford.protege.webprotege.entity.EntityNode;
import edu.stanford.protege.webprotege.pagination.Page;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Comparator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 13 Sep 2018
 */
public class GetHierarchySiblingsActionHandler extends AbstractProjectActionHandler<GetHierarchySiblingsAction, GetHierarchySiblingsResult> {

    @Nonnull
    private final HierarchyProviderMapper hierarchyProviderMapper;

    @Nonnull
    private final GraphNodeRenderer nodeRenderer;

    @Nonnull
    private final DictionaryManager dictionaryManager;

    @Inject
    public GetHierarchySiblingsActionHandler(@Nonnull AccessManager accessManager,
                                             @Nonnull HierarchyProviderMapper hierarchyProviderMapper,
                                             @Nonnull GraphNodeRenderer nodeRenderer,
                                             @Nonnull DictionaryManager dictionaryManager) {
        super(accessManager);
        this.hierarchyProviderMapper = checkNotNull(hierarchyProviderMapper);
        this.nodeRenderer = checkNotNull(nodeRenderer);
        this.dictionaryManager = checkNotNull(dictionaryManager);
    }

    @Nonnull
    @Override
    public Class<GetHierarchySiblingsAction> getActionClass() {
        return GetHierarchySiblingsAction.class;
    }

    @Nullable
    @Override
    protected BuiltInAction getRequiredExecutableBuiltInAction(GetHierarchySiblingsAction action) {
        return BuiltInAction.VIEW_PROJECT;
    }

    @Nonnull
    @Override
    public GetHierarchySiblingsResult execute(@Nonnull GetHierarchySiblingsAction action, @Nonnull ExecutionContext executionContext) {
        Page<GraphNode<EntityNode>> siblings =
                hierarchyProviderMapper.getHierarchyProvider(action.getHierarchyId())
                        .map(hp -> {
                                 int pageNumber = action.getPageRequest().getPageNumber();
                                 int pageSize = action.getPageRequest().getPageSize();
                                 // Parents to get children that are siblings
                                 return hp.getParents(action.getEntity())
                                         .stream()
                                         // Siblings
                                         .flatMap(par -> hp.getChildren(par).stream())
                                         // Remove self
                                         .filter(sib -> !sib.equals(action.getEntity()))
                                         .distinct()
                                         .sorted(Comparator.comparing(dictionaryManager::getShortForm))
                                         // Paginate and transform
                                         .collect(PageCollector.toPage(pageNumber, pageSize))
                                         .orElse(Page.emptyPage())
                                         .transform(e -> nodeRenderer.toGraphNode(e, hp));
                             }
                        )
                        .orElse(Page.emptyPage());
        return GetHierarchySiblingsResult.create(siblings);
    }
}