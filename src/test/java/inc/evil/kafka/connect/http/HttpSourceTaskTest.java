package inc.evil.kafka.connect.http;

import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTaskContext;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import inc.evil.kafka.connect.http.config.HttpSourceConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class HttpSourceTaskTest {

    private HttpSourceTask task;
    private Map<String, String> baseProps;

    private final SourceTaskContext mockContext = mock(SourceTaskContext.class);
    private final OffsetStorageReader mockOffsetStorageReader = mock(OffsetStorageReader.class);
    private final HttpApiClient mockApiClient = mock(HttpApiClient.class);

    @BeforeEach
    public void setUp() {
        task = new HttpSourceTask();
        task.initialize(mockContext);

        doReturn(mockOffsetStorageReader).when(mockContext).offsetStorageReader();

        baseProps = Map.of(
                "http.url", "example.com",
                "http.method", "GET",
                "http.poll.interval.ms", "5000",
                "topic", "test-topic"
        );

        task.start(baseProps);
        task.setApiClient(mockApiClient);
    }

    @Test
    void version_withTask_returnsExpectedVersion() {
        assertThat(task.version()).isEqualTo(HttpSourceConfig.VERSION);
    }

    @Test
    void start_withNoPreviousOffset_setsLastPollTimeToZero() {
        when(mockOffsetStorageReader.offset(any())).thenReturn(null);

        assertThat(task.getLastPollTime()).isEqualTo(0L);
    }

    @Test
    void start_withPersistedOffset_setsLastPollTimeFromOffset() {
        Map<String, Object> persistedOffset = Collections.singletonMap("last_polled_timestamp", 12345L);
        when(mockOffsetStorageReader.offset(any())).thenReturn(persistedOffset);

        task.start(baseProps);

        assertThat(task.getLastPollTime()).isEqualTo(12345L);
    }

    @Test
    void poll_withElapsedInterval_returnsSourceRecord() throws InterruptedException, IOException, URISyntaxException {
        String apiResponse = "{\"data\": \"some_data\"}";
        when(mockApiClient.executeRequest(any(), any())).thenReturn(apiResponse);
        task.setLastPollTime(System.currentTimeMillis() - 6000L);

        List<SourceRecord> records = task.poll();

        assertThat(records).hasSize(1);
        SourceRecord record = records.get(0);
        assertThat(record.topic()).isEqualTo("test-topic");
        assertThat(record.value()).isEqualTo(apiResponse);
    }

    @Test
    void poll_withinInterval_returnsEmptyList() throws InterruptedException, IOException, URISyntaxException {
        String apiResponse = "{\"data\": \"some_data\"}";
        task.setLastPollTime(System.currentTimeMillis());
        when(mockApiClient.executeRequest(any(), any())).thenReturn(apiResponse);

        List<SourceRecord> records = task.poll();

        assertThat(records).isEmpty();
        verify(mockApiClient, never()).executeRequest(any(), any());
    }

    @Test
    void poll_withIOException_throwsRetriableException() throws IOException, URISyntaxException {
        when(mockApiClient.executeRequest(any(), any())).thenThrow(new IOException("Test IO Exception"));

        assertThatThrownBy(() -> task.poll())
                .isInstanceOf(RetriableException.class)
                .hasMessageContaining("I/O error during HTTP request.");
    }

    @Test
    void poll_withRuntimeException_throwsConnectException() throws IOException, URISyntaxException {
        when(mockApiClient.executeRequest(any(), any())).thenThrow(new RuntimeException("Unexpected test error"));

        assertThatThrownBy(() -> task.poll())
                .isInstanceOf(ConnectException.class)
                .hasMessageContaining("Unexpected error.");
    }

    @Test
    void stop_withTask_closesApiClient() throws IOException {
        task.stop();

        verify(mockApiClient).close();
    }

}
