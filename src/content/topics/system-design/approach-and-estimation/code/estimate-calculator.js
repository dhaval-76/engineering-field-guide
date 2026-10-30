export function estimate({ requestsPerDay, peakFactor = 3, newItemsPerDay, bytesPerItem, replicas = 3, bytesPerResponse }) {
  const avgQps = requestsPerDay / 86_400;
  const peakQps = avgQps * peakFactor;
  return {
    avgQps,
    peakQps,
    storageTbPerYear: (newItemsPerDay * bytesPerItem * 365 * replicas) / 1e12,
    egressMbPerSec: (peakQps * bytesPerResponse) / 1e6,
  };
}

console.log(estimate({
  requestsPerDay: 10e9, newItemsPerDay: 100e6, bytesPerItem: 500, bytesPerResponse: 500,
}));
// { avgQps: 115740.7, peakQps: 347222.2, storageTbPerYear: 54.75, egressMbPerSec: 173.6 }
