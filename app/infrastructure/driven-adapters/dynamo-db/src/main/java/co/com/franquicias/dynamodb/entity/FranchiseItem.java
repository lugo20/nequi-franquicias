package co.com.franquicias.dynamodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.Locale;

/**
 * Single-table item. PK groups a whole franchise; SK identifies the franchise, a branch or a product:
 * FRANCHISE | BRANCH#&lt;branchId&gt; | BRANCH#&lt;branchId&gt;#PRODUCT#&lt;productId&gt;.
 * A separate FRANCHISE_NAME#&lt;lowercase name&gt; item reserves each franchise name.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class FranchiseItem {

    public static final String FRANCHISE = "FRANCHISE";
    public static final String BRANCH = "BRANCH";
    public static final String PRODUCT = "PRODUCT";
    public static final String FRANCHISE_NAME = "FRANCHISE_NAME";
    private static final String SEPARATOR = "#";

    @Getter(onMethod_ = @DynamoDbPartitionKey)
    private String pk;
    @Getter(onMethod_ = @DynamoDbSortKey)
    private String sk;
    private String type;
    private String id;
    private String branchId;
    private String name;
    private Integer stock;

    public static String franchisePk(String franchiseId) {
        return FRANCHISE + SEPARATOR + franchiseId;
    }

    public static String franchiseNamePk(String name) {
        return FRANCHISE_NAME + SEPARATOR + name.toLowerCase(Locale.ROOT);
    }

    public static String branchSk(String branchId) {
        return BRANCH + SEPARATOR + branchId;
    }

    public static String productSk(String branchId, String productId) {
        return branchSk(branchId) + SEPARATOR + PRODUCT + SEPARATOR + productId;
    }
}
