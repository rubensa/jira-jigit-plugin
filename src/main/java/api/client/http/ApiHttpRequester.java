package api.client.http;

import api.APIException;
import com.google.gson.Gson;
import jigit.Function;
import jigit.common.HeaderConsumer;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public final class ApiHttpRequester {
    @NotNull
    private static final String ENCODING = "UTF-8";
    @NotNull
    private static final Gson GSON = new Gson();
    private final int requestTimeout;
    @NotNull
    private final URL url;
    @NotNull
    private HttpMethod httpMethod = HttpMethod.GET;
    @Nullable
    private HeaderConsumer<Map<String, List<String>>> headerConsumer;
    @NotNull
    private final Map<String, String> requestProperties;
    @NotNull
    private final ErrorListener errorListener;

    public ApiHttpRequester(@NotNull URL url,
                            int requestTimeout,
                            @NotNull ErrorListener errorListener,
                            @NotNull Map<String, String> requestProperties) {
        this.url = url;
        this.requestProperties = requestProperties;
        this.requestTimeout = requestTimeout;
        this.errorListener = errorListener;
    }

    @NotNull
    public ApiHttpRequester withMethod(@NotNull HttpMethod method) {
        this.httpMethod = method;
        return this;
    }

    @NotNull
    public ApiHttpRequester withHeaderConsumer(@NotNull HeaderConsumer<Map<String, List<String>>> headersConsumer) {
        this.headerConsumer = headersConsumer;
        return this;
    }

    @Nullable
    public <T> T withResultOf(@NotNull Class<T> type) throws IOException {
        return withResultOf(new ReaderOfClass<>(type));
    }

    @Nullable
    public <T> T withResultOf(@NotNull Type type) throws IOException {
        return withResultOf(new ReaderOfType<T>(type));
    }

    @Nullable
    private <T> T withResultOf(@NotNull Function<Reader, T> streamReaderFunction) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = buildConnection();
            return parse(connection, streamReaderFunction);
        } finally {
            IOUtils.close(connection);
        }
    }

    @NotNull
    private HttpURLConnection buildConnection() throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (requestTimeout > 0) {
            connection.setConnectTimeout(requestTimeout);
            connection.setReadTimeout(requestTimeout);
        }
        connection.setRequestMethod(httpMethod.name());
        for (Map.Entry<String, String> parameter : requestProperties.entrySet()) {
            connection.setRequestProperty(parameter.getKey(), parameter.getValue());
        }
        if (HttpMethod.POST == httpMethod) {
            connection.setDoOutput(true);
        }
        return connection;
    }

    @SuppressWarnings("OverlyBroadCatchBlock")
    @Nullable
    private <T> T parse(@NotNull HttpURLConnection connection, @NotNull Function<Reader, T> streamReaderFunction) throws IOException {
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(connection.getInputStream(), ENCODING);
            if (headerConsumer != null) {
                headerConsumer.accept(connection.getHeaderFields());
            }
            return streamReaderFunction.apply(reader);
        } catch (IOException e) {
            handleError(e, connection);
        } finally {
            IOUtils.closeQuietly(reader);
        }

        return null;
    }

    private void handleError(@NotNull IOException e, @NotNull HttpURLConnection connection) throws IOException {
        if (e instanceof SocketTimeoutException && requestTimeout > 0) {
            throw e;
        }
        errorListener.onError(e, connection);

        InputStream errorStream = null;
        try {
            errorStream = connection.getErrorStream();
            String error = null;
            if (errorStream != null) {
                error = IOUtils.toString(errorStream, ENCODING);
            }
            throw new APIException(error, connection.getResponseCode(), e);
        } finally {
            IOUtils.closeQuietly(errorStream);
        }
    }

    private static final class ReaderOfClass<T> implements Function<Reader, T> {
        private final @NotNull Class<T> type;

        private ReaderOfClass(@NotNull Class<T> type) {
            this.type = type;
        }

        @Override
        public @NotNull T apply(@NotNull Reader reader) {
            return GSON.fromJson(reader, type);
        }
    }

    private static final class ReaderOfType<T> implements Function<Reader, T> {
        private final @NotNull Type type;

        private ReaderOfType(@NotNull Type type) {
            this.type = type;
        }

        @Override
        public @NotNull T apply(@NotNull Reader reader) {
            return GSON.fromJson(reader, type);
        }
    }
}