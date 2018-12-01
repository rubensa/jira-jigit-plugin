package jigit.common;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public enum UrlActions {
    instance;

    @NotNull
    private static final String ENCODING = "UTF-8";

    public @NotNull String encoded(@NotNull String string) throws UnsupportedEncodingException {
        return URLEncoder.encode(string, UrlActions.ENCODING);
    }

    public @NotNull String withoutTrailingSlash(@NotNull String string) {
        return string.trim().replaceAll("/+$", "");
    }
}
