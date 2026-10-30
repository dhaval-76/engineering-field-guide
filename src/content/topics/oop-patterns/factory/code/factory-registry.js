// A registry that plug-ins add themselves to.
const processors = new Map();

export function registerProcessor(method, processor) {
  if (processors.has(method)) throw new Error(`duplicate processor for ${method}`);
  processors.set(method, processor);
}

export function processorFor(method) {
  const p = processors.get(method);
  if (!p) throw new Error(`unsupported payment method ${method}`);
  return p;
}

// card.js
registerProcessor('CARD', { pay: async (payment) => ({ ok: true }) });
