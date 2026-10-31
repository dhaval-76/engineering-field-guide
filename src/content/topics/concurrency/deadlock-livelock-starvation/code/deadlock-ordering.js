// JavaScript is single-threaded, but async locks (mutexes around awaits) deadlock the
// same way.
import { Mutex } from 'async-mutex';

async function transfer(from, to, amount) {
  // global order
  const [first, second] = [from, to].sort((a, b) => a.id.localeCompare(b.id));
  await first.mutex.runExclusive(() =>
    second.mutex.runExclusive(async () => {
      from.balance -= amount;
      to.balance += amount;
    }),
  );
}
