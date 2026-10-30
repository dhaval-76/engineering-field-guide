import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';

const def = protoLoader.loadSync('api-orders.proto', { keepCase: false, longs: String, enums: String });
const { OrderService } = grpc.loadPackageDefinition(def).shop.orders.v1;

const client = new OrderService('orders.internal:443', grpc.credentials.createSsl(), {
  'grpc.service_config': JSON.stringify({ loadBalancingConfig: [{ round_robin: {} }] }),
});

client.getOrder({ orderId: 'A-1001' }, { deadline: Date.now() + 300 }, (err, order) => {
  if (err?.code === grpc.status.NOT_FOUND) return console.log('no such order');
  if (err?.code === grpc.status.DEADLINE_EXCEEDED) return console.log('too slow — retry or degrade');
  if (err) throw err;
  console.log(order.status, order.totalCents);
});

// Server streaming
const stream = client.watchOrder({ orderId: 'A-1001' });
stream.on('data', (event) => console.log('status', event.status));
stream.on('error', (e) => console.error(e.code, e.details));
