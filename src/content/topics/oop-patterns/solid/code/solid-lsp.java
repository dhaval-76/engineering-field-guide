// ✗ Penguin can't honour Bird's contract, so code that works for "any Bird" crashes.
class Bird { void fly() { /* ... */ } }
class Penguin extends Bird {
    @Override void fly() { throw new UnsupportedOperationException(); }
}
// 💥 when a Penguin is in the list
void migrate(List<Bird> flock) { flock.forEach(Bird::fly); }

// ✓ Model the capability separately; every subtype can now do everything its type
// promises.
abstract class Bird { abstract void move(); }
interface Flyer { void fly(); }

class Sparrow extends Bird implements Flyer {
    void move() { fly(); }
    public void fly() { /* flap */ }
}
class Penguin extends Bird {
    void move() { /* swim */ }
}
void migrate(List<? extends Flyer> flyers) { flyers.forEach(Flyer::fly); }
