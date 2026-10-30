interface FahrenheitSource { double readingF(); }        // what the chart library expects (target)
interface CelsiusSensor    { double temperatureC(); }   // what we have (adaptee)

final class CelsiusToFahrenheit implements FahrenheitSource {
    private final CelsiusSensor sensor;
    CelsiusToFahrenheit(CelsiusSensor sensor) { this.sensor = sensor; }
    public double readingF() { return sensor.temperatureC() * 9.0 / 5.0 + 32; }
}

// chart.plot(new CelsiusToFahrenheit(roomSensor));   neither side changed
