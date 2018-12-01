package jigit.indexer.api.gitlab;

import api.APIException;
import jigit.client.gitlab.GitLabRepositoryAPI;
import jigit.client.gitlab.dto.GitLabCommit;
import jigit.client.gitlab.dto.GitLabFile;
import jigit.indexer.api.CommitAdapter;
import jigit.indexer.api.CommitFileAdapter;
import jigit.indexer.api.RequestsCounter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public final class GitLabCommitAdapter implements CommitAdapter {
    @NotNull
    private final GitLabCommit gitlabCommit;
    @NotNull
    private final GitLabRepositoryAPI repositoryAPI;
    @NotNull
    private final RequestsCounter requestsCounter;
    @NotNull
    private final GitLabAPIExceptionHandler apiExceptionHandler;

    public GitLabCommitAdapter(@NotNull GitLabCommit gitlabCommit,
                               @NotNull GitLabRepositoryAPI repositoryAPI,
                               @NotNull RequestsCounter requestsCounter,
                               @NotNull GitLabAPIExceptionHandler apiExceptionHandler) {
        this.gitlabCommit = gitlabCommit;
        this.repositoryAPI = repositoryAPI;
        this.requestsCounter = requestsCounter;
        this.apiExceptionHandler = apiExceptionHandler;
    }

    @NotNull
    @Override
    public String getCommitSha1() {
        return gitlabCommit.getId();
    }

    @NotNull
    @Override
    public String getTitle() {
        return gitlabCommit.getMessage();
    }

    @NotNull
    @Override
    public String getAuthorName() {
        return gitlabCommit.getAuthorName();
    }

    @NotNull
    @Override
    public Date getCreatedAt() {
        return gitlabCommit.getAuthoredDate();
    }

    @NotNull
    @Override
    public Collection<String> getParentSha1s() {
        final List<String> parentSha1s = gitlabCommit.getParentIds();
        return (parentSha1s == null) ? Collections.<String>emptyList() : parentSha1s;
    }

    @NotNull
    @Override
    public Collection<CommitFileAdapter> getCommitDiffs() throws IOException {
        final String commitSha1 = getCommitSha1();
        final GitLabFile[] commitDiffs;
        try {
            commitDiffs = repositoryAPI.commitFiles(commitSha1);
            requestsCounter.increase();
        } catch (APIException e) {
            throw apiExceptionHandler.handle(e);
        }
        if (commitDiffs == null) {
            throw new IllegalStateException("Something went wrong. Got null commit diff for sha1 = " + commitSha1);
        }
        final Collection<CommitFileAdapter> commitFileAdapters = new ArrayList<>();
        for (GitLabFile gitLabFile : commitDiffs) {
            commitFileAdapters.add(new GitLabCommitFileAdapter(gitLabFile));
        }

        return commitFileAdapters;
    }
}
