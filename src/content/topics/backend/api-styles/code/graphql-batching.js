import DataLoader from 'dataloader';

// One set of loaders per request: batching within the request, no cache shared across users.
export function createContext({ user, db }) {
  return {
    user,
    loaders: {
      customer: new DataLoader(async (ids) => {
        const rows = await db.customers.findByIds(ids); // SELECT … WHERE id = ANY($1)
        const byId = new Map(rows.map((c) => [c.id, c]));
        return ids.map((id) => byId.get(id) ?? null); // same order as ids
      }),
    },
  };
}

export const resolvers = {
  Query: {
    order: (_, { id }, { db }) => db.orders.findById(id),
  },
  Order: {
    customer: (order, _, { loaders }) => loaders.customer.load(order.customerId),
  },
  Mutation: {
    cancelOrder: (_, { input }, { db, user }) => db.orders.cancel(input.orderId, user),
  },
};
