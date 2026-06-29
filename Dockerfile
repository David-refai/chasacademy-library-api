# =============================================================================
# Stage 1: Build
# -----------------------------------------------------------------------------
# Uses a full Maven + JDK image so we have the toolchain needed to compile and
# package the application. This stage is discarded after the build — none of
# its layers end up in the final image.
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy only the POM first and resolve dependencies. As long as pom.xml is
# unchanged, Docker reuses this cached layer on subsequent builds instead of
# re-downloading the entire dependency tree every time source code changes.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Now copy the source and build the jar. Tests are skipped here because they
# already run as their own step in the CI pipeline before this image is built.
COPY src ./src
RUN mvn -B clean package -DskipTests

# =============================================================================
# Stage 2: Runtime
# -----------------------------------------------------------------------------
# A minimal JRE-only Alpine image — no JDK, no Maven, no build tools — keeps
# the final image small and reduces the attack surface.
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Run as a dedicated non-root user. If the application is ever compromised,
# the attacker doesn't get root inside the container.
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /build/target/library-api-*.jar app.jar

USER spring:spring

# Default Spring Boot port (server.port is not overridden in application.yml).
EXPOSE 8080

# JWT_SECRET has no default value (see application.yml) and MUST be supplied
# at runtime via `docker run -e JWT_SECRET=...`, never baked into the image.
# REDIS_HOST/PORT and VAULT_HOST/PORT/TOKEN point at the docker-compose
# services and can be overridden the same way.
ENTRYPOINT ["java", "-jar", "app.jar"]
