package co.com.franquicias.dynamodb.config;

import co.com.franquicias.dynamodb.entity.FranchiseItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;

import java.net.URI;

@Configuration
public class DynamoDBConfig {

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient(@Value("${aws.region}") String region,
                                                   @Value("${aws.dynamodb.endpoint:}") String endpoint) {
        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder().region(Region.of(region));
        // Empty in AWS; set only to point to DynamoDB Local.
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(DynamoDbAsyncClient client) {
        return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(client).build();
    }

    @Bean
    public DynamoDbAsyncTable<FranchiseItem> franchiseTable(DynamoDbEnhancedAsyncClient client,
                                                            @Value("${aws.dynamodb.table-name}") String tableName) {
        return client.table(tableName, TableSchema.fromBean(FranchiseItem.class));
    }
}
