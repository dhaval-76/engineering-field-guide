class MiddleStack {
    private class Node {
        int value;
        Node below, above;
    }

    private Node top, middle;
    private int size;

    void push(int value) {
        Node n = new Node();
        n.value = value;
        n.below = top;
        if (top != null) top.above = n;
        top = n;
        size++;
        if (size == 1) middle = n;
        else if (size % 2 == 1) middle = middle.above; // middle moves up on odd sizes
    }

    int pop() {
        int value = top.value;
        if (size % 2 == 1) middle = middle.below;
        top = top.below;
        if (top != null) top.above = null;
        size--;
        return value;
    }

    int top() {
        return top.value;
    }

    int middle() {
        return middle.value;
    }
}
