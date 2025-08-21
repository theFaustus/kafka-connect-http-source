package inc.evil.kafka.connect.http;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.junit.jupiter.api.Test;
import inc.evil.kafka.connect.http.config.HttpSourceConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpSourceConnectorTest {

    private final HttpSourceConnector connector = new HttpSourceConnector();

    @Test
    void version_withConnector_returnsExpectedVersion() {
        String version = connector.version();

        assertThat(version).isEqualTo(HttpSourceConfig.VERSION);
    }

    @Test
    void config_withConnector_returnsNonNullConfigDef() {
        ConfigDef configDef = connector.config();

        assertThat(configDef).isNotNull();
    }

    @Test
    void taskClass_withConnector_returnsHttpSourceTaskClass() {
        Class<? extends Task> taskClass = connector.taskClass();

        assertThat(taskClass).isEqualTo(HttpSourceTask.class);
    }

    @Test
    void startAndTaskConfigs_withValidProps_returnsConfigsForMaxTasks() {
        Map<String, String> configProps = new HashMap<>();
        configProps.put("http.url", "http://test.com/api");
        configProps.put("topic", "test-topic");

        connector.start(configProps);

        int maxTasks = 3;
        List<Map<String, String>> taskConfigs = connector.taskConfigs(maxTasks);

        assertThat(taskConfigs).hasSize(maxTasks);

        for (Map<String, String> taskConfig : taskConfigs) {
            assertThat(taskConfig).isEqualTo(configProps);
        }
    }

}

