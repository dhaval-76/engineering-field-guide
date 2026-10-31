// Fare policy as a function (strategy). Money in paise; distances in km; durations in
// minutes.
export const standardFare = (rates) => ({ vehicle, km, minutes }, surge) => {
  const r = rates[vehicle];
  const raw = r.base + r.perKm * km + r.perMinute * minutes;
  return Math.max(Math.round(raw * surge), r.minimumFare);
};

// Surge per area cell = demand / supply over the last few minutes, smoothed and capped.
export function surgeMultiplier(openRequests, availableDrivers) {
  if (availableDrivers === 0) return 2.5;
  return Math.min(2.5, Math.max(1.0, 0.8 + 0.4 * (openRequests / availableDrivers)));
}
