interface Pizza {
    String description();

    int pricePaise();
}

record Base(String name, int pricePaise) implements Pizza {
    public String description() {
        return name;
    }
}

/** Decorator: a topping wraps any pizza, including one that already has toppings. */
abstract class Topping implements Pizza {
    protected final Pizza inner;

    Topping(Pizza inner) {
        this.inner = inner;
    }
}

class Cheese extends Topping {
    Cheese(Pizza p) {
        super(p);
    }

    public String description() {
        return inner.description() + ", cheese";
    }

    public int pricePaise() {
        return inner.pricePaise() + 5000;
    }
}

class Olives extends Topping {
    Olives(Pizza p) {
        super(p);
    }

    public String description() {
        return inner.description() + ", olives";
    }

    public int pricePaise() {
        return inner.pricePaise() + 3000;
    }
}

class Jalapeno extends Topping {
    Jalapeno(Pizza p) {
        super(p);
    }

    public String description() {
        return inner.description() + ", jalapeño";
    }

    public int pricePaise() {
        return inner.pricePaise() + 2500;
    }
}

/** Builder keeps the wrapping order out of the caller's code. */
class PizzaBuilder {
    private Pizza pizza;

    PizzaBuilder(Base base) {
        pizza = base;
    }

    PizzaBuilder with(Function<Pizza, Pizza> topping) {
        pizza = topping.apply(pizza);
        return this;
    }

    Pizza build() {
        return pizza;
    }
}
