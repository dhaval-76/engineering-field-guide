// Chain of Responsibility: each handler decides to handle, reject, or pass along.
interface Handler { Response handle(Request req, Chain next); }
interface Chain { Response proceed(Request req); }

final class Pipeline implements Chain {
    private final List<Handler> handlers; private final int index; private final Function<Request, Response> endpoint;
    Pipeline(List<Handler> h, Function<Request, Response> endpoint) { this(h, 0, endpoint); }
    private Pipeline(List<Handler> h, int i, Function<Request, Response> e) { handlers = h; index = i; endpoint = e; }
    public Response proceed(Request req) {
        return index == handlers.size() ? endpoint.apply(req)
                                        : handlers.get(index).handle(req, new Pipeline(handlers, index + 1, endpoint));
    }
}

Handler auth = (req, next) -> req.hasValidToken() ? next.proceed(req) : Response.status(401);
Handler rateLimit = (req, next) -> limiter.tryAcquire(req.clientId()) ? next.proceed(req) : Response.status(429);
// new Pipeline(List.of(auth, rateLimit), controller::handle).proceed(request);
// Servlet filters, Spring Security's filter chain and Express middleware all work this way.
