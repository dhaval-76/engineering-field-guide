// Door states and events; anything not listed leaves the state unchanged.
const TRANSITIONS = {
  CLOSED: { OPEN_REQUESTED: 'OPENING' },
  OPENING: { OPENED: 'OPEN' },
  OPEN: { CLOSE_REQUESTED: 'CLOSING', TIMEOUT: 'CLOSING' },
  CLOSING: { OBSTRUCTION: 'OPENING', OPEN_REQUESTED: 'OPENING', CLOSED_FULLY: 'CLOSED' }, // safety: always re-open
};

export class Door {
  state = 'CLOSED';
  on(event) {
    this.state = TRANSITIONS[this.state][event] ?? this.state;
    return this.state;
  }
}
