package inc.evil.kafka.connect.http;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import inc.evil.kafka.connect.http.config.HttpSourceConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static inc.evil.kafka.connect.http.config.HttpSourceConfig.VERSION;

/**
 * An HTTP Source connector that provides data from an HTTP endpoint to a Kafka topic.
 * <p>
 * This connector is responsible for starting and stopping the connector tasks, as well as providing
 * the configuration for the tasks. It acts as the entry point for the Kafka Connect framework
 * to manage the data ingestion process.
 * </p>
 */
public class HttpSourceConnector extends SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(HttpSourceConnector.class);

    private Map<String, String> configProps = null;

    /**
     * Get the version of this connector.
     *
     * @return The version string, usually defined as a constant.
     */
    @Override
    public String version() {
        return VERSION;
    }

    /**
     * Define the configuration for this connector.
     *
     * @return A {@link ConfigDef} object that describes the configuration parameters.
     */
    @Override
    public ConfigDef config() {
        return HttpSourceConfig.getConfig();
    }

    /**
     * Start this connector. This method is called by the Kafka Connect framework when the
     * connector is first started. It is used to initialize any necessary resources, such as
     * storing the configuration properties.
     *
     * @param props The configuration properties for this connector.
     */
    @Override
    public void start(Map<String, String> props) {
        log.info("Starting HttpSourceConnector {}", props);
        configProps = props;
    }

    /**
     * Stop this connector. This method is called by the Kafka Connect framework when the
     * connector is being stopped. It is used to clean up any resources initialized in the
     * {@link #start(Map)} method.
     */
    @Override
    public void stop() {
        log.info("Stopping HttpSourceConnector");
    }

    /**
     * Get the class of the task that this connector will create.
     *
     * @return The {@link Class} of the {@link Task} to be instantiated.
     */
    @Override
    public Class<? extends Task> taskClass() {
        return HttpSourceTask.class;
    }

    /**
     * Get the configuration for the connector's tasks.
     * <p>
     * This method is responsible for distributing the connector's configuration among the
     * requested number of tasks. In this implementation, each task receives the same
     * configuration.
     * </p>
     *
     * @param maxTasks The maximum number of tasks that should be configured.
     * @return A list of task configurations, where each element in the list is a Map
     * of configuration properties for a single task.
     */
    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        List<Map<String, String>> taskConfigs = new ArrayList<>();
        for (int i = 0; i < maxTasks; i++) {
            taskConfigs.add(configProps);
        }
        return taskConfigs;
    }

}
