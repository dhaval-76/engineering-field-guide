# DynamoDB (boto3)
seat = table.get_item(Key={"pk": "show#991", "sk": "seat#C7"}, ConsistentRead=True)

table.put_item(
    Item={"pk": "show#991", "sk": "seat#C7", "holder": user_id},
    # ConditionalCheckFailedException if taken
    ConditionExpression="attribute_not_exists(holder)",
)

# MongoDB (pymongo)
from pymongo import ReadPreference
from pymongo.read_concern import ReadConcern
from pymongo.write_concern import WriteConcern

orders = db.get_collection(
    "orders",
    write_concern=WriteConcern(w="majority"),
    read_concern=ReadConcern("majority"),
    read_preference=ReadPreference.PRIMARY,
)
products = db.get_collection("products", read_preference=ReadPreference.SECONDARY_PREFERRED)
