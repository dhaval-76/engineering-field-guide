class BoundedBuffer<T> {
    private final Deque<T> items = new ArrayDeque<>();
    private final int capacity;

    BoundedBuffer(int capacity) { this.capacity = capacity; }

    public synchronized void put(T item) throws InterruptedException {
        // WHILE, not if — re-check after every wakeup
        while (items.size() == capacity) {
            // releases the monitor so a consumer can take()
            wait();
        }
        items.addLast(item);
        notifyAll();                         // wake consumers waiting for "not empty"
    }

    public synchronized T take() throws InterruptedException {
        while (items.isEmpty()) {
            wait();
        }
        T item = items.removeFirst();
        notifyAll();                         // wake producers waiting for "not full"
        return item;
    }
}
// notifyAll, not notify: producers and consumers wait on the same monitor, and notify()
// could wake another producer while the only thing that can make progress is a consumer.
