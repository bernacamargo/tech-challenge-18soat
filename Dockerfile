# Imagem multi-stage da API da oficina.
# Publicada no GHCR pelo release.yml (ADR 0019) e consumida pelos manifests em /k8s (ADR 0025).
# Compose local continua com build: . (docker-compose.yml, perfil full).

# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Runtime
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_OPTS=""
EXPOSE 8080
# Health no cluster: probes HTTP do kubelet em /actuator/health/liveness|readiness.
# Health no compose: healthcheck do serviço app (wget no container).
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
