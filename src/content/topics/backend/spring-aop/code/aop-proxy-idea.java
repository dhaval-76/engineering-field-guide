// What Spring does for you, by hand: a JDK dynamic proxy that times every call of an interface
@SuppressWarnings("unchecked")
static <T> T timed(T target, Class<T> iface) {
    return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface},
        (proxy, method, args) -> {
            long start = System.nanoTime();
            try {
                return method.invoke(target, args);            // the real call
            } catch (InvocationTargetException e) {
                throw e.getCause();                           // rethrow the original exception
            } finally {
                log.info("{} took {} µs", method.getName(), (System.nanoTime() - start) / 1_000);
            }
        });
}

OrderService service = timed(new DefaultOrderService(repo), OrderService.class);
// Spring Boot uses CGLIB subclass proxies by default (they also work without interfaces) —
// which is why final classes and final/private methods can't be advised.
