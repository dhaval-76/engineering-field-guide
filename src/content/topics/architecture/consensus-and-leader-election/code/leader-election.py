# python-etcd3: a leader lease. Only one node can create the key while its lease is alive.
import time
import etcd3

client = etcd3.client(host="etcd-0", port=2379)
lease = client.lease(ttl=10)                                     # 10 s TTL

won, _ = client.transaction(
    compare=[client.transactions.create("/election/billing-scheduler") == 0],   # key doesn't exist yet
    success=[client.transactions.put("/election/billing-scheduler", NODE_ID, lease=lease)],
    failure=[],
)
if won:
    _, meta = client.get("/election/billing-scheduler")
    fencing_token = meta.mod_revision          # increases with every new leader: pass it with each write
    start_leading(fencing_token)
    while leading():
        lease.refresh()                        # keep-alive; if it stops, the key expires and others can win
        time.sleep(3)
else:
    watch_and_retry()                          # watch the key; campaign again when it's deleted
# Simpler in practice: use your platform's election (Kubernetes Lease objects, a DB lease row).
