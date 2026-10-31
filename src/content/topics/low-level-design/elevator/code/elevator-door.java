enum DoorState {
    CLOSED,
    OPENING,
    OPEN,
    CLOSING
}

enum DoorEvent {
    OPEN_REQUESTED,
    OPENED,
    CLOSE_REQUESTED,
    TIMEOUT,
    OBSTRUCTION,
    CLOSED_FULLY
}

final class Door {
    private DoorState state = DoorState.CLOSED;

    synchronized DoorState on(DoorEvent e) {
        state =
                switch (state) {
                    case CLOSED -> e == DoorEvent.OPEN_REQUESTED ? DoorState.OPENING : state;
                    case OPENING -> e == DoorEvent.OPENED ? DoorState.OPEN : state;
                    case OPEN ->
                            (e == DoorEvent.CLOSE_REQUESTED || e == DoorEvent.TIMEOUT)
                                    ? DoorState.CLOSING
                                    : state;
                    case CLOSING ->
                            switch (e) {
                                // safety: always re-open
                                case OBSTRUCTION, OPEN_REQUESTED -> DoorState.OPENING;
                                case CLOSED_FULLY -> DoorState.CLOSED;
                                default -> state;
                            };
                };
        return state;
    }

    // Convenience used by the simulation in ElevatorCar
    synchronized boolean isOpen() {
        return state == DoorState.OPEN;
    }

    synchronized void open() {
        state = DoorState.OPEN;
    }

    synchronized void close() {
        state = DoorState.CLOSED;
    }
}
