# HTTP Source Connector for Apache Kafka Connect 🔌

This is a very simple Kafka Connect source connector that polls a configurable HTTP endpoint at regular intervals and publishes the response body to a Kafka topic. It's designed for simple data ingestion from REST APIs.

## Features ✨

* **Configurable HTTP method:** Supports GET, POST, PUT, PATCH, and DELETE.
* **Customizable headers and query parameters:** Allows you to pass additional information with your requests.
* **Proxy support:** Allows you to pass proxy host and port if needed.
* **Authentication options:** Supports Basic Authentication and Bearer tokens.
* **Timeout management:** Configure connection and read timeouts to prevent long-running requests.
* **Polling interval:** Sets a frequency for fetching new data.
* **Idempotency:** The connector uses the URL as part of the source partition to ensure each unique URL is processed independently. It also tracks the last successful poll time to avoid re-polling too frequently.

-----

## Prerequisites 🛠️

* Apache Kafka and Kafka Connect
* Java 11
* Maven

-----

## Building the Connector 🏗️

To build a fat JAR of the connector, run the following Maven command from the project's root directory:

```bash
  mvn clean package
```

This will create a JAR file in the `target/` directory, which can then be deployed to your Kafka Connect environment.

-----

## Deployment and Usage 🚀

### 1\. Set up the Environment

The easiest way to get started is by using a containerized Kafka Connect environment, such as the `landoop/fast-data-dev` image. This image includes Kafka, Zookeeper, and Kafka Connect, making it a great choice for local development.

```bash
  docker run -it --rm  -p 2181:2181 -p 3030:3030 -p 8081-8083:8081-8083 -p 9092:9092 -v "C:\Users\your-user\kafka-connect-http:/connectors/kafka-connect-http" landoop/fast-data-dev:latest
```

This command starts the container and maps the `kafka-connect-http` directory from your local machine to the container's `/connectors/kafka-connect-http` directory. This is where you should place the fat JAR you built in the previous step.

### 2\. Verify Plugin Installation

Once the container is running, verify that the connector plugin is available to Kafka Connect by listing the installed plugins.

```bash
  curl -X GET http://localhost:8083/connector-plugins | jq
```

You should see `inc.evil.kafka.connect.http.HttpSourceConnector` in the list.

### 3\. Create the Connector Instance

Create a new instance of the connector using the REST API. Modify the `config` block with your specific settings.

```bash
    curl -X POST http://localhost:3030/api/kafka-connect/connectors \
      -H "Content-Type: application/json" \
      -d '{
        "name": "my-connector",
        "config": {
          "connector.class": "inc.evil.kafka.connect.http.HttpSourceConnector",
          "http.headers": "Content-Type=application/json",
          "tasks.max": "1",
          "http.query.params": "random=123",
          "http.proxy.host": "https://foo.com",
          "http.proxy.port": "1234",
          "http.url": "https://httpbin.org/get",
          "http.read.timeout.ms": "10000",
          "topic": "my-topic",
          "http.poll.interval.ms": "10000",
          "http.connect.timeout.ms": "5000",
          "http.method": "GET",
          "value.converter": "org.apache.kafka.connect.storage.StringConverter",
          "key.converter": "org.apache.kafka.connect.storage.StringConverter"
        }
      }' | jq
```

### 4\. Check the Connector Status

After creating the connector, you can check its status and the status of its tasks.

```bash
  curl -X GET http://localhost:8083/connectors/kafka-connect-http/status | jq
```

### 5\. Monitor the Logs

To troubleshoot or monitor the connector's activity, check the Kafka Connect logs within the container.

```bash
  tail -100 /var/log/connect-distributed.log
```

### 6\. Delete the Connector

To stop and remove the connector instance, use the DELETE endpoint.

```bash
  curl -X DELETE http://localhost:3030/api/kafka-connect/connectors/kafka-connect-http | jq
```

### 7\. View all active connectors

To see all active connectors use the following command.

```bash
  curl -X GET http://localhost:3030/api/kafka-connect/connectors
```

-----

## Configuration Options ⚙️

| Name                      | Type        | Importance | Default                       | Description                                                                                        |
|:--------------------------|:------------|:-----------|:------------------------------|:---------------------------------------------------------------------------------------------------|
| `topic`                   | `STRING`    | `HIGH`     | N/A                           | The Kafka topic to write the fetched data to.                                                      |
| `http.url`                | `STRING`    | `HIGH`     | `"https://httpbin.org/get"`   | The base HTTP URL to fetch data from.                                                              |
| `http.proxy.host`         | `STRING`    | `MEDIUM`   | `""`                          | Optional HTTP proxy host to route requests through.                                                |
| `http.proxy.port`         | `INT`       | `MEDIUM`   | `-1`                          | Optional HTTP proxy port. Must be set if `http.proxy.host` is provided.                            |
| `http.poll.interval.ms`   | `INT`       | `HIGH`     | `60000`                       | Polling interval in milliseconds between consecutive HTTP requests. Minimum allowed is 5000 ms.    |
| `http.method`             | `STRING`    | `HIGH`     | `GET`                         | The HTTP method to use for requests. Valid values are `GET`, `POST`, `PATCH`, `PUT`, `DELETE`.     |
| `http.connect.timeout.ms` | `INT`       | `MEDIUM`   | `5000`                        | Timeout in milliseconds for establishing the HTTP connection.                                      |
| `http.read.timeout.ms`    | `INT`       | `MEDIUM`   | `10000`                       | Timeout in milliseconds for reading the HTTP response.                                             |
| `http.headers`            | `STRING`    | `LOW`      | `""`                          | Optional HTTP request headers in 'key=value' pairs separated by commas.                            |
| `http.query.params`       | `STRING`    | `LOW`      | `""`                          | Optional query parameters appended to the HTTP request URL in 'key=value' pairs separated by '&'.  |
| `http.request.body`       | `STRING`    | `MEDIUM`   | `""`                          | The HTTP request body to be sent with the request. Only applicable for methods like POST and PUT.  |
| `http.auth.username`      | `STRING`    | `MEDIUM`   | `""`                          | Username for HTTP Basic Authentication. Used with `http.auth.password`.                            |
| `http.auth.password`      | `PASSWORD`  | `MEDIUM`   | `""`                          | Password for HTTP Basic Authentication. Used with `http.auth.username`.                            |
| `http.auth.bearer`        | `PASSWORD`  | `MEDIUM`   | `""`                          | Bearer token for the Authorization header.                                                         |
