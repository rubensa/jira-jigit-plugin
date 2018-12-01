package jigit.client.gitlab;

import org.jetbrains.annotations.NotNull;

public enum GitLabApiVersion {
    v3("/api/v3"),
    v4("/api/v4");

    private final @NotNull String apiPath;

    GitLabApiVersion(@NotNull String apiPath) {
        this.apiPath = apiPath;
    }

    public @NotNull String apiPath() {
        return apiPath;
    }
}
