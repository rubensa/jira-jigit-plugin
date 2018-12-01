package jigit.indexer.repository;

import jigit.Function;
import jigit.client.github.GitHub;
import jigit.client.github.GitHubOrganizationsAPI;
import jigit.client.github.GitHubRepositoryAPI;
import jigit.client.gitlab.GitLab;
import jigit.client.gitlab.GitLabGroupsAPI;
import jigit.client.gitlab.GitLabRepositoryAPI;
import jigit.common.UrlActions;
import jigit.indexer.api.APIAdapter;
import jigit.indexer.api.github.GitHubErrorListener;
import jigit.indexer.api.github.GithubAPIAdapter;
import jigit.indexer.api.gitlab.GitLabAPIAdapter;
import jigit.indexer.api.gitlab.GitLabAPIExceptionHandler;
import jigit.indexer.api.gitlab.GitLabErrorListener;
import jigit.settings.JigitRepo;
import jigit.settings.JigitSettingsManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

public enum ServiceType {
    GitLab("GitLab") {
        @Override
        @NotNull
        public Collection<RepoInfo> repositories(@NotNull JigitRepo repo,
                                                 @NotNull JigitSettingsManager settingsManager) throws IOException {
            return gitlabRepositories(repo, settingsManager, "/api/v4");
        }
    },
    GitLab8("GitLab 8 or earlier") {
        @Override
        @NotNull
        public Collection<RepoInfo> repositories(@NotNull JigitRepo repo,
                                                 @NotNull JigitSettingsManager settingsManager) throws IOException {
            return gitlabRepositories(repo, settingsManager, "/api/v3");
        }
    },
    GitHub("GitHub") {
        @Override
        @NotNull
        public Collection<RepoInfo> repositories(@NotNull JigitRepo repo,
                                                 @NotNull JigitSettingsManager settingsManager) throws IOException {
            final String serverUrl = UrlActions.instance.withoutTrailingSlash(repo.getServerUrl());
            final String apiUrl = ServiceType.isGitHubSite(serverUrl) ?
                    SITE_API_URL : (serverUrl + ENTERPRISE_API_SUFFIX);
            final GitHub gitHub = new GitHub(apiUrl, repo.getToken(),
                    repo.getRequestTimeout(), new GitHubErrorListener(settingsManager, repo));

            return repo.getRepoType().repositories(repo, new GitHubOrganizationsAPI(gitHub), new Function<String, APIAdapter>() {
                @NotNull
                @Override
                public APIAdapter apply(@NotNull String arg) {
                    final GitHubRepositoryAPI repositoryAPI = gitHub.repositoryApi(arg);
                    return new GithubAPIAdapter(repositoryAPI);
                }
            });
        }
    };

    public static final @NotNull Collection<ServiceType> values =
            Collections.unmodifiableList(Arrays.asList(ServiceType.values()));
    private static final @NotNull String SITE_API_URL = "https://api.github.com";
    private static final @NotNull String ENTERPRISE_API_SUFFIX = "/api/v3";
    private static final @NotNull Pattern GITHUB_URL_REGEXP = Pattern.compile("^.+github.com.*$", Pattern.CASE_INSENSITIVE);

    private final @NotNull String displayName;

    ServiceType(@NotNull String displayName) {
        this.displayName = displayName;
    }

    @NotNull
    private static Collection<RepoInfo> gitlabRepositories(@NotNull JigitRepo repo,
                                                           @NotNull JigitSettingsManager settingsManager,
                                                           @NotNull String apiPath) throws IOException {
        final GitLab gitLab = new GitLab(UrlActions.instance.withoutTrailingSlash(repo.getServerUrl()) + apiPath,
                repo.getToken(), repo.getRequestTimeout(), GitLabErrorListener.INSTANCE);
        final GitLabGroupsAPI groupsAPI = new GitLabGroupsAPI(gitLab);

        final GitLabAPIExceptionHandler apiExceptionHandler = new GitLabAPIExceptionHandler(settingsManager, repo);
        return repo.getRepoType().repositories(repo, groupsAPI, new Function<String, APIAdapter>() {
            @NotNull
            @Override
            public APIAdapter apply(@NotNull String arg) {
                final GitLabRepositoryAPI repositoryAPI = gitLab.repositoryAPI(arg);
                return new GitLabAPIAdapter(repositoryAPI, apiExceptionHandler);
            }
        });
    }

    public static boolean isGitHubSite(@NotNull String serverUrl) {
        return GITHUB_URL_REGEXP.matcher(serverUrl).matches();
    }

    @SuppressWarnings("unused") //used in velocity
    public @NotNull String getDisplayName() {
        return displayName;
    }

    public abstract @NotNull Collection<RepoInfo> repositories(@NotNull JigitRepo repo,
                                                               @NotNull JigitSettingsManager settingsManager) throws IOException;
}
