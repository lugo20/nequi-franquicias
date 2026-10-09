package co.com.franquicias.dynamodb.config;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamoDBConfigTest {

    private final DynamoDBConfig config = new DynamoDBConfig();

    @Test
    void usesCustomEndpointWhenProvided() {
        try (DynamoDbAsyncClient client = config.dynamoDbAsyncClient("us-east-1", "http://localhost:8000")) {
            assertEquals(Optional.of(URI.create("http://localhost:8000")),
                    client.serviceClientConfiguration().endpointOverride());
        }
    }

    @Test
    void usesDefaultAwsEndpointWhenBlank() {
        try (DynamoDbAsyncClient client = config.dynamoDbAsyncClient("us-east-1", "")) {
            assertEquals(Optional.empty(), client.serviceClientConfiguration().endpointOverride());
        }
    }
}
