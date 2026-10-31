sealed interface Shape permits Circle, Rectangle, Triangle {
    double area();
}
record Circle(double r) implements Shape          { public double area() { return Math.PI * r * r; } }
record Rectangle(double w, double h) implements Shape { public double area() { return w * h; } }
record Triangle(double b, double h) implements Shape  { public double area() { return 0.5 * b * h; } }

// Runtime polymorphism (dynamic dispatch): one call site, the right method chosen per
// object.
double total(List<Shape> shapes) {
    double sum = 0;
    for (Shape s : shapes) sum += s.area();
    return sum;
}

// Compile-time polymorphism (overloading): the compiler picks by parameter types.
class Printer {
    void print(int n)    { System.out.println("int " + n); }
    void print(String s) { System.out.println("string " + s); }
}
