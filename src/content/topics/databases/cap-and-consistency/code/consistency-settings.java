// DynamoDB (AWS SDK v2): reads are eventually consistent unless you ask.
GetItemResponse seat = dynamo.getItem(GetItemRequest.builder()
    .tableName("seats")
    .key(Map.of("pk", AttributeValue.fromS("show#991"), "sk", AttributeValue.fromS("seat#C7")))
    .consistentRead(true)                 // strongly consistent: reads the leader of the partition
    .build());

// Conditional write = compare-and-set on one item: only reserve if still free.
dynamo.putItem(PutItemRequest.builder()
    .tableName("seats")
    .item(Map.of("pk", AttributeValue.fromS("show#991"), "sk", AttributeValue.fromS("seat#C7"),
                 "holder", AttributeValue.fromS(userId)))
    .conditionExpression("attribute_not_exists(holder)")   // ConditionalCheckFailedException if taken
    .build());

// MongoDB (Java driver): durability and freshness per collection or operation.
MongoCollection<Document> orders = db.getCollection("orders")
    .withWriteConcern(WriteConcern.MAJORITY)          // acknowledged by a majority: survives failover
    .withReadConcern(ReadConcern.MAJORITY)            // only data a majority has acknowledged
    .withReadPreference(ReadPreference.primary());    // latest data

MongoCollection<Document> catalogue = db.getCollection("products")
    .withReadPreference(ReadPreference.secondaryPreferred());   // stale is fine: lower latency
// Causal consistency for one user's session: ClientSession with causallyConsistent(true)

// Cassandra (DataStax driver 4): per statement.
session.execute(SimpleStatement.newInstance("SELECT * FROM carts WHERE user_id = ?", userId)
    .setConsistencyLevel(DefaultConsistencyLevel.LOCAL_QUORUM));
