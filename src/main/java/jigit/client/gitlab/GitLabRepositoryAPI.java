package jigit.client.gitlab;

import com.google.gson.reflect.TypeToken;
import jigit.client.gitlab.dto.GitLabBranch;
import jigit.client.gitlab.dto.GitLabCommit;
import jigit.client.gitlab.dto.GitLabFile;
import jigit.common.NextPage;
import jigit.common.NextPageFactory;
import jigit.common.PageParam;
import jigit.common.UrlActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GitLabRepositoryAPI {
    private static final @NotNull Type LIST_OF_BRANCHES = new TypeToken<List<GitLabBranch>>() {
    }.getType();
    private static final @NotNull String PROJECTS_PATH = "projects";
    private static final @NotNull String BRANCHES_PATH = "repository/branches";
    private static final @NotNull String COMMITS_PATH = "repository/commits";
    private static final @NotNull String DIFF_PATH = "diff";
    private final @NotNull String repositoryPath;
    private final @NotNull GitLab gitLab;

    public GitLabRepositoryAPI(@NotNull String repository, @NotNull GitLab gitLab) {
        this.gitLab = gitLab;
        try {
            this.repositoryPath = '/' + PROJECTS_PATH + '/' + UrlActions.instance.encoded(repository);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Nullable
    public GitLabCommit commit(@NotNull String sha1) throws IOException {
        return gitLab.get(repositoryPath + '/' + COMMITS_PATH + '/' + sha1)
                .withResultOf(GitLabCommit.class);
    }

    @Nullable
    public GitLabBranch branch(@NotNull String branchName) throws IOException {
        return gitLab.get(repositoryPath + '/' + BRANCHES_PATH + '/' + UrlActions.instance.encoded(branchName))
                .withResultOf(GitLabBranch.class);
    }

    @NotNull
    public Collection<GitLabBranch> branches() throws IOException {
        final Set<GitLabBranch> result = new LinkedHashSet<>();
        final NextPageFactory nextPageFactory = new NextPageFactory(
                new NextPage(
                        gitLab.fullPath(repositoryPath + '/' + BRANCHES_PATH + '?' + PageParam.MAX)
                )
        );

        while (nextPageFactory.getNextPage().getUrl() != null) {
            final List<GitLabBranch> values =
                    gitLab.get(new URL(nextPageFactory.getNextPage().getUrl()))
                            .withHeaderConsumer(nextPageFactory)
                            .withResultOf(LIST_OF_BRANCHES);
            if (values != null) {
                result.addAll(values);
            }
        }
        return result;
    }

    @Nullable
    public GitLabFile[] commitFiles(@NotNull String sha1) throws IOException {
        return gitLab.get(repositoryPath + '/' + COMMITS_PATH + '/' + sha1 + '/' + DIFF_PATH)
                .withResultOf(GitLabFile[].class);
    }
}
