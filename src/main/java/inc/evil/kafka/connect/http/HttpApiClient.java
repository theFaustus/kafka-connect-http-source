package inc.evil.kafka.connect.http;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.connect.errors.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import inc.evil.kafka.connect.http.config.HttpSourceConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * A reusable HTTP client for making requests.
 * It is configured to handle timeouts, headers, and different authentication methods.
 * This class is designed to be used by Kafka Connect tasks to fetch data from HTTP endpoints.
 */
public class HttpApiClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(HttpApiClient.class);

    private final String requestParams;
    private final String requestBody;
    private final String headers;
    private final String authUsername;
    private final String authPassword;
    private final String authBearer;
    private final String proxyHost;
    private final int proxyPort;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    private CloseableHttpClient httpClient;

    /**
     * Initializes the client with configuration parameters.
     *
     * @param config The configuration object containing client settings.
     */
    public HttpApiClient(AbstractConfig config) {
        this.requestParams = config.getString(HttpSourceConfig.HTTP_QUERY_PARAMS);
        this.requestBody = config.getString(HttpSourceConfig.HTTP_REQUEST_BODY);
        this.headers = config.getString(HttpSourceConfig.HTTP_HEADERS);
        this.authUsername = config.getString(HttpSourceConfig.HTTP_AUTH_USERNAME);
        this.authPassword = config.getPassword(HttpSourceConfig.HTTP_AUTH_PASSWORD).value();
        this.authBearer = config.getPassword(HttpSourceConfig.HTTP_AUTH_BEARER).value();
        this.connectTimeoutMs = config.getInt(HttpSourceConfig.HTTP_CONNECT_TIMEOUT_MS);
        this.readTimeoutMs = config.getInt(HttpSourceConfig.HTTP_READ_TIMEOUT_MS);
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .setResponseTimeout(readTimeoutMs, TimeUnit.MILLISECONDS);

        this.proxyHost = config.getString(HttpSourceConfig.HTTP_PROXY_HOST);
        this.proxyPort = config.getInt(HttpSourceConfig.HTTP_PROXY_PORT);
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            requestConfigBuilder.setProxy(proxy);
        }

        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfigBuilder.build())
                .build();
    }

    /**
     * Executes an HTTP request and returns the response body as a String.
     *
     * @param baseUri The base URI of the HTTP endpoint.
     * @param method  The HTTP method (e.g., "GET", "POST").
     * @return The response body as a String if the request is successful.
     * @throws IOException        If an I/O error occurs during the request.
     * @throws URISyntaxException If the provided URI is invalid.
     */
    public String executeRequest(String baseUri, String method) throws IOException, URISyntaxException {
        log.info("Polling API at {}", baseUri);
        return httpClient.execute(createHttpRequest(baseUri, Method.normalizedValueOf(method.toUpperCase())), new StringResponseHandler());
    }

    /**
     * Creates an HttpUriRequestBase object based on the provided URI and method.
     * Adds configured headers and authentication.
     *
     * @param baseUri The base URI for the request.
     * @param method  The HTTP method to use.
     * @return A configured HttpUriRequestBase object.
     * @throws URISyntaxException If the URI is invalid.
     */
    private HttpUriRequestBase createHttpRequest(String baseUri, Method method) throws URISyntaxException {
        URI uri = buildUriWithParams(baseUri);
        HttpUriRequestBase request;
        switch (method) {
            case GET:
                request = new HttpGet(uri);
                break;
            case POST:
                request = new HttpPost(uri);
                addBody(request);
                break;
            case PUT:
                request = new HttpPut(uri);
                addBody(request);
                break;
            case PATCH:
                request = new HttpPatch(uri);
                addBody(request);
                break;
            case DELETE:
                request = new HttpDelete(uri);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported HTTP method: " + method);
        }
        addHeaders(request);
        addAuth(request);
        log.debug("Computed HTTP request={}", request);
        return request;
    }

    /**
     * Adds a request body to the given HTTP request if applicable.
     * <p>
     * Only HTTP methods that support bodies (POST, PUT, PATCH) will have
     * the body attached. For GET and DELETE requests, this method does nothing.
     *
     * @param request The HTTP request to which the body should be added.
     */
    private void addBody(HttpUriRequestBase request) {
        if (requestBody == null || requestBody.isEmpty()) {
            return;
        }
        if (request instanceof HttpPost || request instanceof HttpPut || request instanceof HttpPatch) {
            StringEntity entity = new StringEntity(requestBody, StandardCharsets.UTF_8);
            request.setEntity(entity);
        }
    }

    /**
     * Builds a URI by appending query parameters from the configuration.
     *
     * @param baseUri The base URI string.
     * @return A URI object with parameters appended.
     * @throws URISyntaxException If the URI is invalid.
     */
    private URI buildUriWithParams(String baseUri) throws URISyntaxException {
        if (requestParams == null || requestParams.isEmpty()) {
            return new URI(baseUri);
        }

        StringBuilder sb = new StringBuilder(baseUri);
        if (baseUri.contains("?")) {
            sb.append("&");
        } else {
            sb.append("?");
        }
        sb.append(requestParams);

        return new URI(sb.toString());
    }

    /**
     * Adds custom headers from a comma-separated string to the request.
     * Headers are expected in the format "HeaderName=Value,AnotherHeader=AnotherValue".
     *
     * @param request The request object to add headers to.
     */
    private void addHeaders(HttpUriRequestBase request) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        String[] headersArray = headers.split(",");
        for (String header : headersArray) {
            String[] parts = header.split("=", 2);
            if (parts.length == 2) {
                request.addHeader(parts[0].trim(), parts[1].trim());
            } else {
                log.warn("Skipping invalid header format: {}", header);
            }
        }
    }

    /**
     * Adds authentication headers (Bearer or Basic) to the request based on configuration.
     *
     * @param request The request object.
     */
    private void addAuth(HttpUriRequestBase request) {
        if (authBearer != null && !authBearer.isEmpty()) {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authBearer);
        } else if (authUsername != null && !authUsername.isEmpty() && authPassword != null && !authPassword.isEmpty()) {
            String auth = authUsername + ":" + authPassword;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
        }
    }

    /**
     * Closes the underlying HTTP client, releasing all resources.
     *
     * @throws IOException If an I/O error occurs during client shutdown.
     */
    @Override
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    /**
     * A simple response handler that consumes the response entity and returns
     * its content as a String, while also handling status codes.
     */
    static class StringResponseHandler implements HttpClientResponseHandler<String> {
        /**
         * Handles the response, checking the status code and converting the entity to a String.
         *
         * @param response The HTTP response to handle.
         * @return The response body as a String.
         * @throws IOException If an I/O error occurs.
         */
        @Override
        public String handleResponse(ClassicHttpResponse response) throws IOException {
            int statusCode = response.getCode();
            if (statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_REDIRECTION) {
                HttpEntity entity = response.getEntity();
                try {
                    return EntityUtils.toString(entity);
                } catch (ParseException e) {
                    throw new ConnectException("Failed to parse HTTP response.", e);
                }
            } else {
                throw new ConnectException("HTTP request failed with status code: " + statusCode);
            }
        }
    }

    /**
     * A setter for the HTTP client. Used primarily for unit testing to inject a mock client.
     *
     * @param httpClient The mock or real HTTP client instance.
     */
    void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

}
