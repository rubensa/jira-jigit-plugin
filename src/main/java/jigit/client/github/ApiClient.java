package jigit.client.github;

import api.client.http.ApiHttpRequester;
import api.client.http.ErrorListener;
import api.client.http.HttpMethod;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public abstract class ApiClient {
    @NotNull
    private final String apiUrl;

    protected ApiClient(@NotNull String apiUrl) {
        this.apiUrl = apiUrl;
        try {
            new URL(apiUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @NotNull
    public ApiHttpRequester get(@NotNull String endpointUrl) throws IOException {
        return apiHttpRequester(apiUrl(endpointUrl));
    }

    @NotNull
    public ApiHttpRequester get(@NotNull URL url) throws IOException {
        return apiHttpRequester(url);
    }

    @NotNull
    public String fullPath(@NotNull String url) {
        final String maybeSlash = url.startsWith("/") ? "" : "/";
        return apiUrl + maybeSlash + url;
    }

    @NotNull
    private ApiHttpRequester apiHttpRequester(@NotNull URL url) {
        return new ApiHttpRequester(url, requestTimeout(), errorListener(), requestParameters());
    }

    @NotNull
    protected abstract ErrorListener errorListener();

    @NotNull
    public ApiHttpRequester post(@NotNull String endpointUrl) throws IOException {
        return apiHttpRequester(apiUrl(endpointUrl)).withMethod(HttpMethod.POST);
    }

    protected abstract int requestTimeout();

    @NotNull
    protected abstract Map<String, String> requestParameters();

    @NotNull
    private URL apiUrl(@NotNull String url) throws IOException {
        return new URL(fullPath(url));
    }
}
