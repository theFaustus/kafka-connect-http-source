package inc.evil.kafka.connect.http.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpMethodRecommenderTest {

    private HttpMethodRecommender recommender;

    @BeforeEach
    public void setUp() {
        recommender = new HttpMethodRecommender();
    }

    @Test
    void validValues_withHttpMethodField_returnsValidMethods() {
        String fieldName = "http.method";
        Map<String, Object> parsedConfig = Collections.emptyMap();

        List<Object> validValues = recommender.validValues(fieldName, parsedConfig);

        assertThat(validValues).containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE");
    }

    @Test
    void visible_withHttpMethodField_returnsTrue() {
        String fieldName = "http.method";
        Map<String, Object> parsedConfig = Collections.emptyMap();

        boolean isVisible = recommender.visible(fieldName, parsedConfig);

        assertThat(isVisible).isTrue();
    }
}
