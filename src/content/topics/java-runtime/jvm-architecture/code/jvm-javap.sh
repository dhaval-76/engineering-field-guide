# Compile and look at the bytecode the JVM actually runs
cat > Add.java <<'JAVA'
public class Add {
    static int add(int a, int b) { return a + b; }
    public static void main(String[] args) { System.out.println(add(2, 3)); }
}
JAVA
javac Add.java
javap -c Add.class
#   static int add(int, int);
#     Code:
#        0: iload_0        // push local 0 (a) onto the operand stack
#        1: iload_1        // push local 1 (b)
#        2: iadd           // pop two ints, push their sum
#        3: ireturn        // return the int on top of the stack
java Add               # 5

# See what the JIT compiles (diagnostic; verbose):
java -XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation Add | head
