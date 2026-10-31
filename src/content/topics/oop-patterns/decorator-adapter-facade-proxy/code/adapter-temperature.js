// What the chart library expects (target): { readingF() }. What we have (adaptee): { temperatureC() }.
const celsiusToFahrenheit = (sensor) => ({ readingF: () => (sensor.temperatureC() * 9) / 5 + 32 });

// chart.plot(celsiusToFahrenheit(roomSensor));   neither side changed
