package jigit.client.github;

import api.client.http.ErrorListener;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class GitHub extends ApiClient {
    private static final @NotNull List<String> TIME_FORMATS = Arrays.asList("yyyy/MM/dd HH:mm:ss ZZZZ", "yyyy-MM-dd\'T\'HH:mm:ss\'Z\'");
    private final @NotNull String oauthToken;
    private final int requestTimeout;
    private final @NotNull ErrorListener errorListener;

    public GitHub(@NotNull String serverUrl, @NotNull String oauthToken, int requestTimeout,
                  @NotNull ErrorListener errorListener) {
        super(serverUrl);
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
