package jigit.client.gitlab;

import api.client.http.ErrorListener;
import jigit.client.github.ApiClient;
import jigit.common.UrlActions;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.DatatypeConverter;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

public final class GitLab extends ApiClient {
    private final @NotNull String privateToken;
    private final int requestTimeout;
    private final @NotNull ErrorListener errorListener;

    public GitLab(@NotNull String serverUrl,
                  @NotNull GitLabApiVersion apiVersion,
                  @NotNull String privateToken,
                  int requestTimeout,
                  @NotNull ErrorListener errorListener) {
        super(UrlActions.instance.withoutTrailingSlash(serverUrl) + apiVersion.apiPath());
        this.privateToken = privateToken;
        this.requestTimeout = requestTimeout;
        this.errorListener = errorListener;
    }

    @NotNull
    public static Date parseDate(@NotNull String representation) {
        try {
            return DatatypeConverter.parseDateTime(representation).getTime();
        } catch (Throwable ignore) {
            throw new IllegalStateException("Unable to parse the timestamp: " + representation);
        }
    }

    @NotNull
    public GitLabRepositoryAPI repositoryAPI(@NotNull String repository) {
        return new GitLabRepositoryAPI(repository, this);
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
        return Collections.singletonMap("PRIVATE-TOKEN", privateToken);
    }
}
