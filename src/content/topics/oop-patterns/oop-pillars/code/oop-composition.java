// ✗ Inheritance for code reuse: Stack exposes every List method, so callers
//   can insert in the middle and break LIFO. (java.util.Stack extends Vector — a known mistake.)
class BadStack<T> extends ArrayList<T> {
    void push(T t) { add(t); }
    T pop() { return remove(size() - 1); }
}

// ✓ Composition: Stack HAS-A Deque and exposes only stack operations.
final class Stack<T> {
    private final Deque<T> items = new ArrayDeque<>();
    public void push(T item) { items.push(item); }
    public T pop()           { return items.pop(); }
    public T peek()          { return items.peek(); }
    public boolean isEmpty() { return items.isEmpty(); }
}
