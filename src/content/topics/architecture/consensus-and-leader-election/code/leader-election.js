// etcd3: campaign for leadership; the library keeps the lease alive.
import { Etcd3 } from 'etcd3';

const client = new Etcd3({ hosts: ['etcd-0:2379', 'etcd-1:2379', 'etcd-2:2379'] });
const election = client.election('billing-scheduler', 10); // 10 s lease TTL
const campaign = election.campaign(process.env.NODE_ID);

campaign.on('elected', () => startLeading());
campaign.on('error', (err) => {
  stopLeading(); // lease lost or connection broken: stop acting as leader immediately
  console.error('lost leadership', err);
});

process.on('SIGTERM', async () => {
  await campaign.resign(); // hand over quickly instead of waiting for the lease to expire
  process.exit(0);
});
