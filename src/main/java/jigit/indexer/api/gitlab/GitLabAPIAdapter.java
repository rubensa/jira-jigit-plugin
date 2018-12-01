package jigit.indexer.api.gitlab;

import api.APIException;
import jigit.client.gitlab.GitLabRepositoryAPI;
import jigit.client.gitlab.dto.GitLabBranch;
import jigit.client.gitlab.dto.GitLabCommit;
import jigit.indexer.api.APIAdapter;
import jigit.indexer.api.CommitAdapter;
import jigit.indexer.api.RequestsCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GitLabAPIAdapter implements APIAdapter {
    @NotNull
    private final GitLabRepositoryAPI repositoryAPI;
    @NotNull
    private final GitLabAPIExceptionHandler apiExceptionHandler;
    @NotNull
    private final RequestsCounter requestsCounter = new RequestsCounter();

    public GitLabAPIAdapter(@NotNull GitLabRepositoryAPI repositoryAPI,
                            @NotNull GitLabAPIExceptionHandler apiExceptionHandler) {
        this.repositoryAPI = repositoryAPI;
        this.apiExceptionHandler = apiExceptionHandler;
    }

    @Override
    @NotNull
    public CommitAdapter getCommit(@NotNull String commitSha1) throws IOException {
        final GitLabCommit commit;
        try {
            commit = repositoryAPI.commit(commitSha1);
            requestsCounter.increase();
        } catch (APIException e) {
            throw apiExceptionHandler.handle(e);
        }
        if (commit == null) {
            throw new IllegalStateException("Something went wrong. Got null commit for sha1 = " + commitSha1);
        }
        return new GitLabCommitAdapter(commit, repositoryAPI, requestsCounter, apiExceptionHandler);
    }

    @Override
    @Nullable
    public String getHeadCommitSha1(@NotNull String branch) throws IOException {
        try {
            final GitLabBranch gitLabBranch = repositoryAPI.branch(branch);
            requestsCounter.increase();
            return (gitLabBranch == null) ? null : gitLabBranch.getId();
        } catch (APIException e) {
            throw apiExceptionHandler.handle(e);
        }
    }

    @Override
    public long getRequestsQuantity() {
        return requestsCounter.value();
    }

    @NotNull
    @Override
    public List<String> branches() throws IOException {
        final List<String> branches = new ArrayList<>();
        requestsCounter.increase();
        for (GitLabBranch branch : repositoryAPI.branches()) {
            branches.add(branch.getName());
        }
        return Collections.unmodifiableList(branches);
    }
}

