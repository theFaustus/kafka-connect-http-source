package inc.evil.kafka.connect.http;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.errors.ConnectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HttpApiClientTest {

    @Mock
    private CloseableHttpClient mockHttpClient;

    @Mock
    private ClassicHttpResponse mockResponse;

    private HttpApiClient apiClient;

    private static class DummyConfig extends AbstractConfig {
        public DummyConfig(Map<String, String> originalProps) {
            super(new ConfigDef()
                            .define("http.query.params", ConfigDef.Type.STRING, "", ConfigDef.Importance.LOW, "HTTP query parameters")
                            .define("http.request.body", ConfigDef.Type.STRING, "", ConfigDef.Importance.LOW, "HTTP request body")
                            .define("http.headers", ConfigDef.Type.STRING, "", ConfigDef.Importance.LOW, "HTTP headers")
                            .define("http.auth.username", ConfigDef.Type.STRING, "", ConfigDef.Importance.LOW, "HTTP auth username")
                            .define("http.auth.password", ConfigDef.Type.PASSWORD, "", ConfigDef.Importance.LOW, "HTTP auth password")
                            .define("http.auth.bearer", ConfigDef.Type.PASSWORD, "", ConfigDef.Importance.LOW, "HTTP auth bearer token")
                            .define("http.connect.timeout.ms", ConfigDef.Type.INT, 10000, ConfigDef.Importance.LOW, "Connection timeout")
                            .define("http.read.timeout.ms", ConfigDef.Type.INT, 30000, ConfigDef.Importance.LOW, "Read timeout"),
                    originalProps
            );
        }
    }

    @BeforeEach
    public void setUp() {
        Map<String, String> props = Collections.singletonMap("http.url", "http://example.com");
        DummyConfig config = new DummyConfig(props);
        apiClient = new HttpApiClient(config);
        apiClient.setHttpClient(mockHttpClient);
    }

    @Test
    void executeRequest_withSuccessfulGet_returnsResponseBody() throws IOException, URISyntaxException, ParseException {
        String expectedResponse = "{\"status\":\"ok\"}";
        when(mockResponse.getCode()).thenReturn(HttpStatus.SC_OK);
        when(mockResponse.getEntity()).thenReturn(new StringEntity(expectedResponse));
        when(mockHttpClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    HttpClientResponseHandler<String> handler = invocation.getArgument(1);
                    return handler.handleResponse(mockResponse);
                });

        String responseBody = apiClient.executeRequest("http://example.com", "GET");

        assertThat(responseBody).isEqualTo(expectedResponse);

        verify(mockHttpClient).execute(any(HttpGet.class), any(HttpClientResponseHandler.class));
    }

    @Test
    void executeRequest_withAuthHeaders_executesSuccessfully() throws Exception {
        Map<String, String> props = Map.of(
                "http.auth.username", "testuser",
                "http.auth.password", "testpass"
        );
        DummyConfig config = new DummyConfig(props);
        HttpApiClient authApiClient = new HttpApiClient(config);

        java.lang.reflect.Field field = HttpApiClient.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(authApiClient, mockHttpClient);

        when(mockResponse.getCode()).thenReturn(HttpStatus.SC_OK);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("auth_success"));
        when(mockHttpClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    HttpClientResponseHandler<String> handler = invocation.getArgument(1);
                    return handler.handleResponse(mockResponse);
                });

        authApiClient.executeRequest("http://example.com", "GET");

        verify(mockHttpClient).execute(any(HttpGet.class), any(HttpClientResponseHandler.class));
    }

    @Test
    void executeRequest_withFailedRequest_throwsConnectException() throws IOException {
        when(mockResponse.getCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(mockHttpClient.execute(any(HttpUriRequestBase.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    HttpClientResponseHandler<String> handler = invocation.getArgument(1);
                    return handler.handleResponse(mockResponse);
                });

        assertThatThrownBy(() -> apiClient.executeRequest("http://example.com", "GET"))
                .isInstanceOf(ConnectException.class);
    }

    @Test
    void close_withApiClient_closesHttpClient() throws IOException {
        apiClient.close();

        verify(mockHttpClient).close();
    }

}
