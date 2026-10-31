// execute(Runnable): fire-and-forget; an exception goes to the thread's
// UncaughtExceptionHandler (often just logged)
pool.execute(() -> sendEmail(order));

// submit(Callable): returns a Future; get() re-throws the task's failure as
// ExecutionException
Future<Receipt> f = pool.submit(() -> charge(order));
try {
    Receipt r = f.get(2, TimeUnit.SECONDS);           // always bound waits
} catch (TimeoutException e) {
    f.cancel(true);                                   // interrupt the task
} catch (ExecutionException e) {
    throw new PaymentFailed(e.getCause());            // the real exception is the cause
}

// invokeAll: run a batch and wait for all
List<Future<Price>> prices =
        pool.invokeAll(List.of(() -> priceA(), () -> priceB()), 1, TimeUnit.SECONDS);
