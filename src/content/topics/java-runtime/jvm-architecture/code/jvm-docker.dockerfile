# Build with a JDK, run with a JRE: the runtime image has no compiler and is much smaller.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system app
USER app
WORKDIR /app
COPY --from=build /src/target/app.jar app.jar
# Size the heap relative to the container's memory limit instead of a fixed -Xmx:
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
