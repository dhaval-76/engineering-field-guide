// DynamoDB (AWS SDK v3, DocumentClient)
import { GetCommand, PutCommand } from '@aws-sdk/lib-dynamodb';

const seat = await ddb.send(new GetCommand({
  TableName: 'seats',
  Key: { pk: 'show#991', sk: 'seat#C7' },
  ConsistentRead: true,                       // default false = eventually consistent
}));

await ddb.send(new PutCommand({
  TableName: 'seats',
  Item: { pk: 'show#991', sk: 'seat#C7', holder: userId },
  ConditionExpression: 'attribute_not_exists(holder)',   // fails if someone holds it
}));

// MongoDB (Node driver)
const orders = db.collection('orders', {
  writeConcern: { w: 'majority' },
  readConcern: { level: 'majority' },
  readPreference: 'primary',
});
const products = db.collection('products', { readPreference: 'secondaryPreferred' });
