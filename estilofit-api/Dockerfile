# ── Estágio 1: build do jar (usa o Gradle wrapper do projeto) ────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache de dependências: copia só o necessário para resolver o build primeiro
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Código-fonte e empacotamento (sem rodar testes — eles exigem Docker/Testcontainers)
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ── Estágio 2: runtime enxuto ────────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Usuário não-root
RUN groupadd --system app && useradd --system --gid app app

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
