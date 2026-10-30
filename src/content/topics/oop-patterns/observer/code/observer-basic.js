// Node's EventEmitter (or the browser's EventTarget) is the Observer pattern built in.
import { EventEmitter } from 'node:events';

export class StockTicker extends EventEmitter {
  #prices = new Map();

  update(symbol, price) {
    const old = this.#prices.get(symbol);
    this.#prices.set(symbol, price);
    this.emit('price', { symbol, old, price });
  }
}

const ticker = new StockTicker();
const onPrice = ({ symbol, old, price }) => console.log(symbol, old, '→', price);
ticker.on('price', onPrice);
ticker.update('INFY', 1520);
ticker.off('price', onPrice); // always remove listeners you no longer need
