package inc.evil.kafka.connect.http;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import inc.evil.kafka.connect.http.config.HttpSourceConfig;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * An HTTP Source task that pulls data from a specified HTTP endpoint
 * and publishes it to a Kafka topic.
 * <p>
 * This task is responsible for the actual data ingestion logic. It handles
 * the HTTP requests, polls the endpoint at a configurable interval, and
 * creates {@link SourceRecord} objects to be sent to Kafka. It also manages
 * the offset to track the last successful poll time.
 * </p>
 */
public class HttpSourceTask extends SourceTask {

    private static final Logger log = LoggerFactory.getLogger(HttpSourceTask.class);

    private String url;
    private String method;
    private long pollIntervalMs;
    private String topic;
    private HttpApiClient apiClient;
    private Map<String, String> sourcePartition;
    private Map<String, Object> sourceOffset;
    private long lastPollTime = 0L;

    /**
     * Get the version of this task.
     *
     * @return The version string, usually defined as a constant.
     */
    @Override
    public String version() {
        return HttpSourceConfig.VERSION;
    }

    /**
     * Start the task. This method is called by the Kafka Connect framework when
     * the task is first started. It's used to parse the configuration,
     * initialize the HTTP client, and read any previously stored offset.
     *
     * @param props The configuration properties for this task.
     */
    @Override
    public void start(Map<String, String> props) {
        log.info("Starting HttpSourceTask with properties: {}", props);
        try {
            HttpSourceConfig config = new HttpSourceConfig(props);
            this.url = config.getString(HttpSourceConfig.HTTP_URL);
            this.method = config.getString(HttpSourceConfig.HTTP_METHOD);
            this.pollIntervalMs = config.getInt(HttpSourceConfig.HTTP_POLL_INTERVAL_MS);
            this.topic = config.getString(HttpSourceConfig.TOPIC);

            this.apiClient = new HttpApiClient(config);
            this.apiClient.start();

            this.sourcePartition = Collections.singletonMap("url", this.url);
            this.sourceOffset = context.offsetStorageReader().offset(this.sourcePartition);
            if (this.sourceOffset != null) {
                log.info("Found persisted offset: {}", this.sourceOffset);
                Long lastPolledTimestamp = (Long) this.sourceOffset.get("last_polled_timestamp");
                if (lastPolledTimestamp != null) {
                    this.lastPollTime = lastPolledTimestamp;
                }
            } else {
                log.info("No previous offset found. Starting from scratch.");
            }

        } catch (ConfigException e) {
            throw new ConnectException("Invalid connector configuration.", e);
        }
    }

    /**
     * Poll for new data from the HTTP endpoint.
     * <p>
     * This method is called repeatedly by the Kafka Connect framework. It checks if
     * the poll interval has passed. If not, it waits. If it has, it executes the
     * HTTP request, creates a {@link SourceRecord} from the response, and returns
     * it. The method handles various exceptions that may occur during the request.
     * </p>
     *
     * @return A list of {@link SourceRecord} objects to be sent to Kafka, or an empty list if no
     * new data is available.
     * @throws InterruptedException if the thread is interrupted while waiting.
     * @throws ConnectException if a non-retriable error occurs.
     * @throws RetriableException if a temporary I/O error occurs that can be retried later.
     */
    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPollTime < pollIntervalMs) {
            long waitTime = pollIntervalMs - (currentTime - lastPollTime);
            log.info("Waiting for {} ms before next poll.", waitTime);
            Thread.sleep(waitTime);
            return Collections.emptyList();
        }

        try {

            String payload = this.apiClient.executeRequest(url, method);
            SourceRecord record = getSourceRecord(payload, currentTime);
            log.debug("Publishing fetched data={}", record);
            this.lastPollTime = currentTime;

            return Collections.singletonList(record);

        } catch (ConnectException e) {
            log.error("API client reported an unrecoverable error.", e);
            throw e;
        } catch (IOException e) {
            log.warn("An I/O error occurred during the HTTP request. This is likely temporary.", e);
            throw new RetriableException("I/O error during HTTP request.", e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during the HTTP request.", e);
            throw new ConnectException("Unexpected error.", e);
        }
    }

    private SourceRecord getSourceRecord(String payload, long currentTime) {
        log.info("Successfully fetched data. Payload size: {}", payload.length());
        return new SourceRecord(
                this.sourcePartition,
                Collections.singletonMap("last_polled_timestamp", currentTime),
                this.topic,
                Schema.STRING_SCHEMA,
                Instant.now().toString(),
                Schema.STRING_SCHEMA,
                payload
        );
    }

    /**
     * Stop the task. This method is called by the Kafka Connect framework when
     * the task is being stopped. It is used to clean up any resources, such as
     * closing the HTTP client.
     */
    @Override
    public void stop() {
        log.info("Stopping HttpSourceTask");
        if (this.apiClient != null) {
            try {
                this.apiClient.close();
            } catch (IOException e) {
                log.error("Failed to close HTTP client.", e);
            }
        }
    }


    /**
     * Get the last successful poll time. This method is primarily for testing.
     *
     * @return The timestamp of the last successful poll.
     */
    public long getLastPollTime() {
        return lastPollTime;
    }

    /**
     * Set the last successful poll time. This method is primarily for testing.
     *
     * @param lastPollTime The new timestamp for the last poll.
     */
    void setLastPollTime(long lastPollTime) {
        this.lastPollTime = lastPollTime;
    }

    /**
     * Set the HTTP API client. This method is primarily for testing.
     *
     * @param apiClient The new HTTP API client instance.
     */
    void setApiClient(HttpApiClient apiClient) {
        this.apiClient = apiClient;
    }
}

