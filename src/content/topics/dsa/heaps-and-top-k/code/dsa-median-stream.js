class MedianFinder {
  low = new Heap((a, b) => a > b);    // max-heap
  high = new Heap();                  // min-heap
  addNum(x) {
    this.low.push(x);
    this.high.push(this.low.pop());
    if (this.high.size > this.low.size) this.low.push(this.high.pop());
  }
  findMedian() {
    if (this.low.size > this.high.size) return this.low.peek();
    return (this.low.peek() + this.high.peek()) / 2;
  }
}
