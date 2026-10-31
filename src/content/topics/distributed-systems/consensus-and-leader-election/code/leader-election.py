# python-etcd3: a leader lease. Only one node can create the key while its lease is alive.
import time
import etcd3

client = etcd3.client(host="etcd-0", port=2379)
lease = client.lease(ttl=10)                                     # 10 s TTL

won, _ = client.transaction(
    # key doesn't exist yet
    compare=[client.transactions.create("/election/billing-scheduler") == 0],
    success=[client.transactions.put("/election/billing-scheduler", NODE_ID, lease=lease)],
    failure=[],
)
if won:
    _, meta = client.get("/election/billing-scheduler")
    # increases with every new leader: pass it with each write
    fencing_token = meta.mod_revision
    start_leading(fencing_token)
    while leading():
        # keep-alive; if it stops, the key expires and others can win
        lease.refresh()
        time.sleep(3)
else:
    # watch the key; campaign again when it's deleted
    watch_and_retry()
# Simpler in practice: use your platform's election (Kubernetes Lease objects, a DB lease
# row).
