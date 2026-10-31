// DynamoDB (AWS SDK v2): reads are eventually consistent unless you ask.
GetItemResponse seat = dynamo.getItem(GetItemRequest.builder()
    .tableName("seats")
    .key(Map.of("pk", AttributeValue.fromS("show#991"), "sk", AttributeValue.fromS("seat#C7")))
    // strongly consistent: reads the leader of the partition
    .consistentRead(true)
    .build());

// Conditional write = compare-and-set on one item: only reserve if still free.
dynamo.putItem(PutItemRequest.builder()
    .tableName("seats")
    .item(Map.of("pk", AttributeValue.fromS("show#991"), "sk", AttributeValue.fromS("seat#C7"),
                 "holder", AttributeValue.fromS(userId)))
    // ConditionalCheckFailedException if taken
    .conditionExpression("attribute_not_exists(holder)")
    .build());

// MongoDB (Java driver): durability and freshness per collection or operation.
MongoCollection<Document> orders = db.getCollection("orders")
    // acknowledged by a majority: survives failover
    .withWriteConcern(WriteConcern.MAJORITY)
    // only data a majority has acknowledged
    .withReadConcern(ReadConcern.MAJORITY)
    .withReadPreference(ReadPreference.primary());    // latest data

MongoCollection<Document> catalogue = db.getCollection("products")
    // stale is fine: lower latency
    .withReadPreference(ReadPreference.secondaryPreferred());
// Causal consistency for one user's session: ClientSession with causallyConsistent(true)

// Cassandra (DataStax driver 4): per statement.
session.execute(SimpleStatement.newInstance("SELECT * FROM carts WHERE user_id = ?", userId)
    .setConsistencyLevel(DefaultConsistencyLevel.LOCAL_QUORUM));
