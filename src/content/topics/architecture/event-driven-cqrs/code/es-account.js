// Events are plain objects with a type. State is the fold of the account's events.
const EMPTY = { id: null, open: false, balance: 0, version: 0 };

export function apply(state, e) {
  switch (e.type) {
    case 'Opened':
      return { id: e.accountId, open: true, balance: 0, version: state.version + 1 };
    case 'Deposited':
      return { ...state, balance: state.balance + e.cents, version: state.version + 1 };
    case 'Withdrawn':
      return { ...state, balance: state.balance - e.cents, version: state.version + 1 };
    default:
      throw new Error(`unknown event ${e.type}`);
  }
}

export const replay = (history) => history.reduce(apply, EMPTY);

// Command handlers: validate against current state, return new events.
export function open(state, accountId, owner) {
  if (state.open) throw new Error('already open');
  return [{ type: 'Opened', accountId, owner }];
}

export function deposit(state, cents) {
  if (!state.open) throw new Error('account not open');
  if (cents <= 0) throw new Error('amount must be positive');
  return [{ type: 'Deposited', cents }];
}

export function withdraw(state, cents) {
  if (!state.open) throw new Error('account not open');
  if (cents > state.balance) throw new Error('insufficient funds');
  return [{ type: 'Withdrawn', cents }];
}

// load → decide → append with expected version (optimistic concurrency)
export async function handleWithdraw(store, accountId, cents) {
  const state = replay(await store.load(accountId));
  await store.append(accountId, state.version, withdraw(state, cents));
}
