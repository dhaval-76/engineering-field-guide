// A map from type to constructor replaces a growing switch.
const CREATORS = new Map([
  ['CAR', (plate) => new Car(plate)],
  ['BIKE', (plate) => new Bike(plate)],
  ['TRUCK', (plate) => new Truck(plate)],
]);

export function createVehicle(type, plate) {
  const create = CREATORS.get(type);
  if (!create) throw new Error(`No creator for ${type}`);
  return create(plate);
}
// Adding SCOOTER: one more map entry.
