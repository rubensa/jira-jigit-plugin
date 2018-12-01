package jigit.client.gitlab;

import com.google.gson.reflect.TypeToken;
import jigit.client.gitlab.dto.GitLabBranch;
import jigit.client.gitlab.dto.GitLabCommit;
import jigit.client.gitlab.dto.GitLabFile;
import jigit.common.UrlActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public final class GitLabRepositoryAPI {
    private static final @NotNull Type LIST_OF_BRANCHES = new TypeToken<List<GitLabBranch>>() {
    }.getType();
    @NotNull
    private static final String PROJECTS_PATH = "projects";
    @NotNull
    private static final String BRANCHES_PATH = "repository/branches";
    @NotNull
    private static final String COMMITS_PATH = "repository/commits";
    @NotNull
    private static final String DIFF_PATH = "diff";
    @NotNull
    private final String repositoryPath;
    @NotNull
    private final GitLab gitLab;

    public GitLabRepositoryAPI(@NotNull String repository, @NotNull GitLab gitLab) {
        this.gitLab = gitLab;
        try {
            this.repositoryPath = '/' + PROJECTS_PATH + '/' + UrlActions.instance.encoded(repository);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Nullable
    public GitLabCommit getCommit(@NotNull String sha1) throws IOException {
        return gitLab.get(repositoryPath + '/' + COMMITS_PATH + '/' + sha1)
                .withResultOf(GitLabCommit.class);
    }

    @Nullable
    public GitLabBranch getBranch(@NotNull String branchName) throws IOException {
        return gitLab.get(repositoryPath + '/' + BRANCHES_PATH + '/' + UrlActions.instance.encoded(branchName))
                .withResultOf(GitLabBranch.class);
    }

    @NotNull
    public List<GitLabBranch> branches() throws IOException {
        final List<GitLabBranch> branches = gitLab.get(repositoryPath + '/' + BRANCHES_PATH).withResultOf(LIST_OF_BRANCHES);
        return branches == null ? Collections.<GitLabBranch>emptyList() : branches;
    }

    @Nullable
    public GitLabFile[] getCommitFiles(@NotNull String sha1) throws IOException {
        return gitLab.get(repositoryPath + '/' + COMMITS_PATH + '/' + sha1 + '/' + DIFF_PATH)
                .withResultOf(GitLabFile[].class);
    }
}
