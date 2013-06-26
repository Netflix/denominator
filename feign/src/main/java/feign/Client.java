package feign;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.net.HttpHeaders.CONTENT_LENGTH;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.io.ByteSink;

import dagger.Lazy;
import feign.Request.Options;

/**
 * Submits HTTP {@link Request requests}. Implementations are expected to be
 * thread-safe.
 */
public interface Client {
    /**
     * Executes a request against its {@link Request#url() url} and returns a
     * response.
     * 
     * @param request
     *            safe to replay.
     * @param options
     *            options to apply to this request.
     * @return connected response, {@link Response.Body} is absent or unread.
     * @throws IOException
     *             on a network error connecting to {@link Request#url()}.
     */
    Response execute(Request request, Options options) throws IOException;

    public static class Default implements Client {
        private final Lazy<SSLSocketFactory> sslContextFactory;

        @Inject
        public Default(Lazy<SSLSocketFactory> sslContextFactory) {
            this.sslContextFactory = sslContextFactory;
        }

        @Override
        public Response execute(Request request, Options options) throws IOException {
            HttpURLConnection connection = convertAndSend(request, options);
            return convertResponse(connection);
        }

        HttpURLConnection convertAndSend(Request request, Options options) throws IOException {
            final HttpURLConnection connection = (HttpURLConnection) new URL(request.url()).openConnection();
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection sslCon = (HttpsURLConnection) connection;
                sslCon.setSSLSocketFactory(sslContextFactory.get());
            }
            connection.setConnectTimeout(options.connectTimeoutMillis());
            connection.setReadTimeout(options.readTimeoutMillis());
            connection.setAllowUserInteraction(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod(request.method());

            Integer contentLength = null;
            for (Entry<String, String> header : request.headers().entries()) {
                if (header.getKey().equals(CONTENT_LENGTH))
                    contentLength = Integer.valueOf(header.getValue());
                connection.addRequestProperty(header.getKey(), header.getValue());
            }

            if (request.body().isPresent()) {
                if (contentLength != null) {
                    connection.setFixedLengthStreamingMode(contentLength);
                } else {
                    connection.setChunkedStreamingMode(8196);
                }
                connection.setDoOutput(true);
                new ByteSink() {
                    public OutputStream openStream() throws IOException {
                        return connection.getOutputStream();
                    }
                }.asCharSink(UTF_8).write(request.body().get());
            }
            return connection;
        }

        Response convertResponse(HttpURLConnection connection) throws IOException {
            int status = connection.getResponseCode();
            String reason = connection.getResponseMessage();

            ImmutableListMultimap.Builder<String, String> headers = ImmutableListMultimap.builder();
            for (Map.Entry<String, List<String>> field : connection.getHeaderFields().entrySet()) {
                // response message
                if (field.getKey() != null)
                    headers.putAll(field.getKey(), field.getValue());
            }

            Integer length = connection.getContentLength();
            if (length == -1)
                length = null;
            InputStream stream;
            if (status >= 400) {
                stream = connection.getErrorStream();
            } else {
                stream = connection.getInputStream();
            }
            Reader body = stream != null ? new InputStreamReader(stream) : null;
            return Response.create(status, reason, headers.build(), body, length);
        }
    }
}
