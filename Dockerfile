# syntax=docker/dockerfile:1.7

# Build stage
FROM eclipse-temurin:17-jdk as builder
WORKDIR /app

# Copy only build metadata first to maximize cache hits for dependencies.
COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B dependency:go-offline

# Copy application sources after dependency resolution.
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jdk
VOLUME /tmp
COPY --from=builder /app/target/kolor_spring-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]