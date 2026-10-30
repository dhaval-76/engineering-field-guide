// 1. Blocking, thread per connection: simple code, one OS thread blocked per client.
try (ServerSocket server = new ServerSocket(8080)) {
    ExecutorService pool = Executors.newFixedThreadPool(200);   // ≤ 200 concurrent clients
    while (true) {
        Socket s = server.accept();
        pool.submit(() -> handle(s));                           // read() blocks this thread
    }
}

// 2. Non-blocking + readiness (what Netty does underneath): one thread, many sockets.
Selector selector = Selector.open();
ServerSocketChannel server = ServerSocketChannel.open().bind(new InetSocketAddress(8080));
server.configureBlocking(false);
server.register(selector, SelectionKey.OP_ACCEPT);
while (true) {
    selector.select();                                          // epoll_wait: sleep until something is ready
    for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
        SelectionKey key = it.next(); it.remove();
        if (key.isAcceptable()) server.accept().configureBlocking(false).register(selector, SelectionKey.OP_READ);
        else if (key.isReadable()) readWithoutBlocking((SocketChannel) key.channel());
    }
}

// 3. Java 21 virtual threads: blocking-style code, but a blocked virtual thread releases its carrier.
try (var executor = Executors.newVirtualThreadPerTaskExecutor();
     ServerSocket ss = new ServerSocket(8080)) {
    while (true) {
        Socket s = ss.accept();
        executor.submit(() -> handle(s));                       // 10,000 clients ≈ 10,000 cheap virtual threads
    }
}
