package inc.evil.kafka.connect.http.config;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;

public class HttpSourceConfig extends AbstractConfig {

    public static final String HTTP_URL = "http.url";
    public static final String HTTP_QUERY_PARAMS = "http.query.params";
    public static final String HTTP_REQUEST_BODY = "http.request.body";
    public static final String HTTP_HEADERS = "http.headers";
    public static final String HTTP_POLL_INTERVAL_MS = "http.poll.interval.ms";
    public static final String HTTP_METHOD = "http.method";
    public static final String HTTP_AUTH_USERNAME = "http.auth.username";
    public static final String HTTP_AUTH_PASSWORD = "http.auth.password";
    public static final String HTTP_AUTH_BEARER = "http.auth.bearer";
    public static final String HTTP_CONNECT_TIMEOUT_MS = "http.connect.timeout.ms";
    public static final String HTTP_READ_TIMEOUT_MS = "http.read.timeout.ms";
    public static final String TOPIC = "topic";
    public static final String VERSION = "1.0";

    public static ConfigDef getConfig() {
        return new ConfigDef()
                .define(TOPIC,
                        ConfigDef.Type.STRING,
                        ConfigDef.Importance.HIGH,
                        "The Kafka topic to write the fetched data to.")
                .define(HTTP_HEADERS,
                        ConfigDef.Type.STRING,
                        "",
                        ConfigDef.Importance.LOW,
                        "Optional HTTP request headers in 'key=value' pairs separated by commas. " +
                        "Example: 'Accept=application/json'.")
                .define(HTTP_QUERY_PARAMS,
                        ConfigDef.Type.STRING,
                        "",
                        ConfigDef.Importance.LOW,
                        "Optional query parameters appended to the HTTP request URL in 'key=value' pairs separated by '&'. " +
                        "Example: 'updatedSince=2023-01-01&limit=100'.")
                .define(HTTP_REQUEST_BODY,
                        ConfigDef.Type.STRING,
                        "",
                        ConfigDef.Importance.MEDIUM,
                        "The HTTP request body to be sent with the request. Only applicable for methods like POST and PUT.")
                .define(HTTP_URL,
                        ConfigDef.Type.STRING,
                        "https://httpbin.org/get",
                        new ConfigDef.NonEmptyString(),
                        ConfigDef.Importance.HIGH,
                        "The base HTTP URL to fetch data from.",
                        "Connection",
                        1,
                        ConfigDef.Width.LONG,
                        HTTP_URL)
                .define(HTTP_POLL_INTERVAL_MS,
                        ConfigDef.Type.INT,
                        60000,
                        ConfigDef.Range.atLeast(5000),
                        ConfigDef.Importance.HIGH,
                        "Polling interval in milliseconds between consecutive HTTP requests. " +
                        "Minimum allowed is 10000 ms (10 seconds). Default is 60000 ms (1 minute).")
                .define(HTTP_METHOD,
                        ConfigDef.Type.STRING,
                        "GET",
                        ConfigDef.ValidString.in("GET", "POST", "PATCH", "PUT", "DELETE"),
                        ConfigDef.Importance.HIGH,
                        "The HTTP method to use for requests.",
                        "Connection",
                        2,
                        ConfigDef.Width.SHORT,
                        HTTP_METHOD)
                .define(HTTP_AUTH_USERNAME,
                        ConfigDef.Type.STRING,
                        "",
                        ConfigDef.Importance.MEDIUM,
                        "Username for HTTP Basic Authentication. Used together with '" + HTTP_AUTH_PASSWORD + "'.")
                .define(HTTP_AUTH_PASSWORD,
                        ConfigDef.Type.PASSWORD,
                        "",
                        ConfigDef.Importance.MEDIUM,
                        "Password for HTTP Basic Authentication. Used together with '" + HTTP_AUTH_USERNAME + "'.")
                .define(HTTP_AUTH_BEARER,
                        ConfigDef.Type.PASSWORD,
                        "",
                        ConfigDef.Importance.MEDIUM,
                        "Bearer token for Authorization header. Example: 'eyJhbGciOi...'.")
                .define(HTTP_CONNECT_TIMEOUT_MS,
                        ConfigDef.Type.INT,
                        5000,
                        ConfigDef.Range.atLeast(1000),
                        ConfigDef.Importance.MEDIUM,
                        "Timeout in milliseconds for establishing the HTTP connection. Default is 5000 ms.")
                .define(HTTP_READ_TIMEOUT_MS,
                        ConfigDef.Type.INT,
                        10000,
                        ConfigDef.Range.atLeast(1000),
                        ConfigDef.Importance.MEDIUM,
                        "Timeout in milliseconds for reading the HTTP response. Default is 10000 ms.");
    }

    public HttpSourceConfig(Map<String, String> originals) {
        super(getConfig(), originals);
    }


}
