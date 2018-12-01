package jigit.client.github;

import com.google.gson.reflect.TypeToken;
import jigit.client.github.dto.GitHubOrganization;
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

public final class GitHubOrganizationsAPI implements GroupAPI {
    private static final @NotNull Type LIST_OF_ORGANIZATIONS = new TypeToken<List<GitHubOrganization>>() {
    }.getType();
    @NotNull
    private static final String ORGS_PATH = "orgs";
    @NotNull
    private final GitHub gitHub;

    public GitHubOrganizationsAPI(@NotNull GitHub gitHub) {
        this.gitHub = gitHub;
    }

    @NotNull
    public Collection<GitHubOrganization> repositories(@NotNull String orgName) throws IOException {
        final Set<GitHubOrganization> result = new LinkedHashSet<>();
        final NextPageFactory nextPageFactory = new NextPageFactory(
                new NextPage(
                        gitHub.fullPath('/' + ORGS_PATH + '/' + UrlActions.instance.encoded(orgName) + "/repos?" + PageParam.MAX)
                )
        );

        while (nextPageFactory.getNextPage().getUrl() != null) {
            final List<GitHubOrganization> values =
                    gitHub.get(new URL(nextPageFactory.getNextPage().getUrl()))
                            .withHeaderConsumer(nextPageFactory)
                            .withResultOf(LIST_OF_ORGANIZATIONS);
            if (values != null) {
                result.addAll(values);
            }
        }
        return result;
    }
}
