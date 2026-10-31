# A dict from type to class replaces a growing if/elif chain (classes are callables).
CREATORS = {"CAR": Car, "BIKE": Bike, "TRUCK": Truck}

def create_vehicle(vehicle_type: str, plate: str):
    try:
        return CREATORS[vehicle_type](plate)
    except KeyError:
        raise ValueError(f"No creator for {vehicle_type}") from None
# Adding SCOOTER: one more dict entry.
