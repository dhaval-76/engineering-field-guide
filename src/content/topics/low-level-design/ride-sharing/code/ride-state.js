const NEXT = {
  REQUESTED: ['DRIVER_ASSIGNED', 'CANCELLED'],
  DRIVER_ASSIGNED: ['DRIVER_ARRIVED', 'REQUESTED', 'CANCELLED'],
  DRIVER_ARRIVED: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['COMPLETED'],
  COMPLETED: [],
  CANCELLED: [],
};

export function transition(ride, next, actor, at = new Date()) {
  if (!NEXT[ride.status].includes(next)) throw new Error(`${ride.status} → ${next} not allowed`);
  return {
    ...ride,
    status: next,
    history: [...ride.history, { from: ride.status, to: next, actor, at }],
  };
}
