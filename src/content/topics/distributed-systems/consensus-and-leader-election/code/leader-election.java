// jetcd: campaign for leadership backed by a lease. Blocks until this node is leader.
Client etcd =
        Client.builder()
                .endpoints("http://etcd-0:2379", "http://etcd-1:2379", "http://etcd-2:2379")
                .build();
long leaseId = etcd.getLeaseClient().grant(10).get().getID(); // 10 s TTL
// heartbeats while alive
etcd.getLeaseClient()
        .keepAlive(
                leaseId,
                new StreamObserver<>() {
                    public void onNext(LeaseKeepAliveResponse r) {}

                    // lost the lease: stop work
                    public void onError(Throwable t) {
                        stopLeading();
                    }

                    public void onCompleted() {
                        stopLeading();
                    }
                });

ByteSequence election = ByteSequence.from("billing-scheduler", StandardCharsets.UTF_8);
CampaignResponse won =
        etcd.getElectionClient()
                .campaign(
                        election,
                        leaseId,
                        ByteSequence.from(nodeId, StandardCharsets.UTF_8))
                .get();

long fencingToken = won.getLeader().getRevision(); // increases with every new leader
// pass the token with every write it makes
startLeading(fencingToken);
// on shutdown: etcd.getElectionClient().resign(won.getLeader()).get();

