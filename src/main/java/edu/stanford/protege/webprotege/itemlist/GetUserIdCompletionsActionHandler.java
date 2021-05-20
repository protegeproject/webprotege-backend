package edu.stanford.protege.webprotege.itemlist;

import edu.stanford.protege.webprotege.dispatch.ApplicationActionHandler;
import edu.stanford.protege.webprotege.dispatch.ExecutionContext;
import edu.stanford.protege.webprotege.dispatch.RequestContext;
import edu.stanford.protege.webprotege.dispatch.RequestValidator;
import edu.stanford.protege.webprotege.dispatch.validators.NullValidator;
import edu.stanford.protege.webprotege.user.UserDetailsManager;
import edu.stanford.protege.webprotege.user.UserId;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 12/05/15
 */
public class GetUserIdCompletionsActionHandler implements ApplicationActionHandler<GetUserIdCompletionsAction, GetPossibleItemCompletionsResult<UserId>> {

    private final UserDetailsManager userDetailsManager;

    @Inject
    public GetUserIdCompletionsActionHandler(UserDetailsManager userDetailsManager) {
        this.userDetailsManager = checkNotNull(userDetailsManager);
    }

    @Nonnull
    @Override
    public Class<GetUserIdCompletionsAction> getActionClass() {
        return GetUserIdCompletionsAction.class;
    }

    @Nonnull
    @Override
    public RequestValidator getRequestValidator(@Nonnull GetUserIdCompletionsAction action, @Nonnull RequestContext requestContext) {
        return NullValidator.get();
    }

    @Nonnull
    @Override
    public GetPossibleItemCompletionsResult<UserId> execute(@Nonnull GetUserIdCompletionsAction action, @Nonnull ExecutionContext executionContext) {
        String completionText = action.getCompletionText();
        List<UserId> result = userDetailsManager.getUserIdsContainingIgnoreCase(completionText, 10);
        Collections.sort(result);
        return GetUserIdCompletionsResult.create(result);
    }
}