package jigit.client.github;

import api.client.http.ErrorListener;
import jigit.common.UrlActions;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public final class GitHub extends ApiClient {
    private static final @NotNull String SITE_API_URL = "https://api.github.com";
    private static final @NotNull String ENTERPRISE_API_SUFFIX = "/api/v3";
    private static final @NotNull List<String> TIME_FORMATS = Arrays.asList("yyyy/MM/dd HH:mm:ss ZZZZ", "yyyy-MM-dd\'T\'HH:mm:ss\'Z\'");
    private static final @NotNull Pattern GITHUB_URL_REGEXP = Pattern.compile("^.+github.com.*$", Pattern.CASE_INSENSITIVE);
    private final @NotNull String oauthToken;
    private final int requestTimeout;
    private final @NotNull ErrorListener errorListener;

    public GitHub(@NotNull String serverUrl, @NotNull String oauthToken, int requestTimeout,
                  @NotNull ErrorListener errorListener) {
        super(isGitHubSite(serverUrl) ? SITE_API_URL
                : (UrlActions.instance.withoutTrailingSlash(serverUrl) + ENTERPRISE_API_SUFFIX));
        this.oauthToken = oauthToken;
        this.requestTimeout = requestTimeout;
        this.errorListener = errorListener;
    }

    @NotNull
    public static Date parseDate(@NotNull String representation) {
        for (String timeFormat : TIME_FORMATS) {
            try {
                final SimpleDateFormat e = new SimpleDateFormat(timeFormat);
                e.setTimeZone(TimeZone.getTimeZone("GMT"));
                return e.parse(representation);
            } catch (ParseException ignore) {
            }
        }
        throw new IllegalStateException("Unable to parse the timestamp: " + representation);
    }

    public static boolean isGitHubSite(@NotNull String serverUrl) {
        return GITHUB_URL_REGEXP.matcher(serverUrl).matches();
    }

    @NotNull
    public GitHubRepositoryAPI repositoryApi(@NotNull String repository) {
        return new GitHubRepositoryAPI(repository, this);
    }

    @NotNull
    @Override
    protected ErrorListener errorListener() {
        return errorListener;
    }

    @Override
    protected int requestTimeout() {
        return requestTimeout;
    }

    @Override
    @NotNull
    protected Map<String, String> requestParameters() {
        return Collections.singletonMap("Authorization", "token " + oauthToken);
    }
}
