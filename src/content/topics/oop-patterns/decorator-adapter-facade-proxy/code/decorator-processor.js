// Higher-order functions are decorators: take a function, return a function with extra behaviour.
const withLogging = (pay) => async (payment) => {
  console.info('pay', payment.id);
  const result = await pay(payment);
  console.info('pay', payment.id, '→', result.status);
  return result;
};

const withRetry = (pay, maxAttempts = 3) => async (payment) => {
  for (let attempt = 1; ; attempt++) {
    try {
      return await pay(payment);
    } catch (err) {
      if (!err.transient || attempt === maxAttempts) throw err;
      await new Promise((r) => setTimeout(r, 100 * 2 ** attempt));
    }
  }
};

export const pay = withLogging(withRetry(cardGateway.pay));
