# syntax=docker/dockerfile:1

# ---------- Stage 1: build the jar ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy only the build definition first so the dependency layer is cached
# until pom.xml changes. The cache mount keeps ~/.m2 between builds too.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q package -DskipTests \
 && cp target/*.jar app.jar

# ---------- Stage 2: slim runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Don't run as root inside the container.
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build --chown=app:app /app/app.jar app.jar
USER app

EXPOSE 8080
# Size the heap from the container's memory limit rather than the host's.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# Healthy once Spring's actuator reports UP (bash /dev/tcp: no curl in the image).
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health HTTP/1.0\r\n\r\n" >&3 && grep -q "\"UP\"" <&3'

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
