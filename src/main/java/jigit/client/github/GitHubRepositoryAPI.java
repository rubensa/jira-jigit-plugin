package jigit.client.github;

import com.google.gson.reflect.TypeToken;
import jigit.client.github.dto.GitHubBranch;
import jigit.client.github.dto.GitHubCommit;
import jigit.common.NextPage;
import jigit.common.NextPageFactory;
import jigit.common.PageParam;
import jigit.common.UrlActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GitHubRepositoryAPI {
    private static final @NotNull Type LIST_OF_BRANCHES = new TypeToken<List<GitHubBranch>>() {
    }.getType();
    private static final @NotNull String REPOS_PATH = "repos";
    private static final @NotNull String COMMITS_PATH = "commits";
    private static final @NotNull String BRANCHES_PATH = "branches";
    private final @NotNull String repository;
    private final @NotNull GitHub gitHub;

    public GitHubRepositoryAPI(@NotNull String repository, @NotNull GitHub gitHub) {
        this.repository = repository.trim().replaceAll("^/+", "").replaceAll("/+$", "");
        this.gitHub = gitHub;
    }

    @Nullable
    public GitHubCommit commit(@NotNull String sha1) throws IOException {
        return gitHub.get('/' + REPOS_PATH + '/' + repository + '/' + COMMITS_PATH + '/' + sha1)
                .withResultOf(GitHubCommit.class);
    }

    @Nullable
    public GitHubBranch branch(@NotNull String branchName) throws IOException {
        return gitHub.get('/' + REPOS_PATH + '/' + repository + '/' + BRANCHES_PATH + '/' + UrlActions.instance.encoded(branchName))
                .withResultOf(GitHubBranch.class);
    }

    @NotNull
    public Collection<GitHubBranch> branches() throws IOException {
        final Set<GitHubBranch> result = new LinkedHashSet<>();
        final NextPageFactory nextPageFactory = new NextPageFactory(
                new NextPage(
                        gitHub.fullPath('/' + REPOS_PATH + '/' + repository + '/' + BRANCHES_PATH + '?' + PageParam.MAX)
                )
        );

        while (nextPageFactory.getNextPage().getUrl() != null) {
            final List<GitHubBranch> values =
                    gitHub.get(new URL(nextPageFactory.getNextPage().getUrl()))
                            .withHeaderConsumer(nextPageFactory)
                            .withResultOf(LIST_OF_BRANCHES);
            if (values != null) {
                result.addAll(values);
            }
        }
        return result;
    }
}
