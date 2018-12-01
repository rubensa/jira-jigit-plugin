package jigit.client.gitlab;

import com.google.gson.reflect.TypeToken;
import jigit.client.gitlab.dto.GitLabProject;
import jigit.common.NextPage;
import jigit.common.NextPageFactory;
import jigit.common.PageParam;
import jigit.common.UrlActions;
import jigit.indexer.api.GroupAPI;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GitLabGroupsAPI implements GroupAPI {
    private static final @NotNull Type LIST_OF_PROJECTS = new TypeToken<List<GitLabProject>>() {
    }.getType();
    private final @NotNull GitLab gitLab;

    public GitLabGroupsAPI(@NotNull GitLab gitLab) {
        this.gitLab = gitLab;
    }

    public @NotNull Collection<GitLabProject> repositories(@NotNull String groupName) throws IOException {
        final Set<GitLabProject> result = new LinkedHashSet<>();
        final NextPageFactory nextPageFactory = new NextPageFactory(
                new NextPage(
                        gitLab.fullPath("/groups/" + UrlActions.instance.encoded(groupName) + "/projects?" + PageParam.MAX)
                )
        );

        while (nextPageFactory.getNextPage().getUrl() != null) {
            final List<GitLabProject> values =
                    gitLab.get(new URL(nextPageFactory.getNextPage().getUrl()))
                            .withHeaderConsumer(nextPageFactory)
                            .withResultOf(LIST_OF_PROJECTS);
            if (values != null) {
                result.addAll(values);
            }

        }
        return result;
    }
}
