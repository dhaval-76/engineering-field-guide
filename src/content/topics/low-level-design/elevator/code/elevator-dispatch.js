// uses ElevatorCar from above
// Cost = floors to travel before reaching the caller, given each car's current direction.
export const nearestSuitableCar = (topFloor) => (call, cars) => {
  const cost = (car) => {
    const here = car.floor, target = call.floor;
    if (car.direction === 'IDLE') return Math.abs(here - target);
    if (car.direction === 'UP')
      return target >= here && call.wants === 'UP' ? target - here : topFloor - here + Math.abs(topFloor - target);
    return target <= here && call.wants === 'DOWN' ? here - target : here + target; // sweep to ground (0), then up
  };
  return cars.reduce((best, c) =>
    cost(c) < cost(best) || (cost(c) === cost(best) && c.pendingStops < best.pendingStops) ? c : best);
};

export class ElevatorController {
  constructor(cars, strategy) {
    Object.assign(this, { cars, strategy });
  }
  hallCall(floor, wants) {
    this.strategy({ floor, wants }, this.cars).addStop(floor);
  }
  carCall(carId, floor) {
    this.cars[carId].addStop(floor);
  }
  tick() {
    this.cars.forEach((c) => c.tick());
  }
}
