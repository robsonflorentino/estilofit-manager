# ── Estágio 1: build do jar (usa o Gradle wrapper do projeto) ────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache de dependências: copia só o necessário para resolver o build primeiro
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
# Normaliza a quebra de linha do wrapper (um clone no Windows pode trazer CRLF, o que
# faz o shebang virar "/bin/sh\r" e o build falhar com "./gradlew: not found").
# O sed remove o \r de TODAS as linhas do script — verificado: só assim o wrapper roda
# num container Linux quando o arquivo veio com CRLF.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Código-fonte e empacotamento (sem rodar testes — eles exigem Docker/Testcontainers)
COPY src ./src
# Limita a memória da JVM do Gradle e desliga o daemon do compilador Kotlin.
# No Docker Desktop do Windows a RAM disponível costuma ser menor; sem isto a
# compilação Kotlin pode estourar o limite e o build morre (OOM) com
# "process /bin/sh -c ./gradlew ... did not complete successfully".
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dkotlin.compiler.execution.strategy=in-process -Dorg.gradle.jvmargs=-Xmx2g"
RUN ./gradlew --no-daemon --no-build-cache clean bootJar -x test

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
