from typing import Protocol

class FahrenheitSource(Protocol):      # what the chart library expects (target)
    def reading_f(self) -> float: ...

class CelsiusSensor(Protocol):         # what we have (adaptee)
    def temperature_c(self) -> float: ...

class CelsiusToFahrenheit:
    def __init__(self, sensor: CelsiusSensor):
        self.sensor = sensor
    def reading_f(self) -> float:
        return self.sensor.temperature_c() * 9 / 5 + 32

# chart.plot(CelsiusToFahrenheit(room_sensor))   neither side changed
