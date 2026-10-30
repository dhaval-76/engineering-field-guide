// Adapter for a callback-style legacy client so the rest of the code can use promises.
export class LegacyGatewayAdapter {
  constructor(gateway) {
    this.gateway = gateway;
  }

  pay(payment) {
    return new Promise((resolve, reject) => {
      this.gateway.initiatePayment(
        { txn_ref: payment.id, amt: String(Math.round(payment.amount * 100)), cur: payment.currency },
        (err, res) => {
          if (err) return reject(err);
          if (res.status === 'S') return resolve({ status: 'SUCCESS', ref: res.gateway_ref });
          if (res.status === 'P') return resolve({ status: 'PENDING', ref: res.gateway_ref });
          resolve({ status: 'FAILED', reason: res.err ?? 'unknown' });
        },
      );
    });
  }
}
