package inc.evil.kafka.connect.http.config;

import org.apache.kafka.common.config.ConfigDef;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HttpMethodRecommender implements ConfigDef.Recommender {
    private static final List<Object> METHODS = Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE");

    @Override
    public List<Object> validValues(String name, Map<String, Object> parsedConfig) {
        return METHODS;
    }

    @Override
    public boolean visible(String name, Map<String, Object> parsedConfig) {
        return true;
    }
}
