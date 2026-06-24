# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Download dependencies before copying source so this layer is cached
# as long as pom.xml and the Maven wrapper are unchanged.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q

COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
# Must be glibc-based: grpc-netty-shaded bundles a native SSL lib (netty-tcnative)
# compiled against glibc; Alpine (musl) causes a SIGSEGV on SSL context init.
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as an unprivileged user
RUN groupadd --system spring && useradd --system --gid spring spring

RUN mkdir -p /opt/cprof && \
    wget -q -O- https://storage.googleapis.com/cloud-profiler/java/latest/profiler_java_agent.tar.gz \
    | tar xzv -C /opt/cprof

USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT [ \
  "java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-agentpath:/opt/cprof/profiler_java_agent.so=-cprof_service=mappics,-cprof_service_version=1.0.0,-cprof_enable_heap_sampling=true", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar" \
]
