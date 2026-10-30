# syntax=docker/dockerfile:1
# ── Build stage: full JDK + Maven wrapper; never shipped ─────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Dependencies change rarely: cache them in their own layer (and in a BuildKit cache mount)
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q package -DskipTests \
 && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination /app

# ── Runtime stage: JRE only, non-root ─────────────────────────────────────────
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
USER 10001
WORKDIR /app
# Spring Boot layers, least to most frequently changing → small image updates on deploy
COPY --from=build /app/dependencies/ ./
COPY --from=build /app/spring-boot-loader/ ./
COPY --from=build /app/snapshot-dependencies/ ./
COPY --from=build /app/application/ ./
# Heap = 75% of the container's memory limit; the rest is for metaspace, threads, buffers
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
# Exec form: the JVM is PID 1 and receives SIGTERM for graceful shutdown
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
