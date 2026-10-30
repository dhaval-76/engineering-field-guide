static int returnsOne() {
    try {
        return 1;                       // value 1 is computed and held…
    } finally {
        System.out.println("finally");  // …this still runs before the method returns
    }
}

@SuppressWarnings("finally")
static int returnsTwo() {
    try {
        return 1;
    } finally {
        return 2;                       // ✗ overrides the try's return AND swallows any exception
    }
}

static void lostException() {
    try {
        throw new IllegalStateException("real problem");
    } finally {
        throw new RuntimeException("cleanup failed");   // ✗ the real problem is gone without a trace
    }
}
// finally is skipped only if the JVM stops (System.exit, crash) or the thread never leaves the try.
