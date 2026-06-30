# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="Oficina Registrar" \
      org.opencontainers.image.description="Runtime image for the Oficina registration Discord service"

RUN apk add --no-cache dumb-init tzdata && \
    addgroup -S -g 10001 app && \
    adduser -S -D -H -u 10001 -h /var/lib/oficina/registrar -s /sbin/nologin -G app app && \
    mkdir -p /opt/oficina /var/lib/oficina/registrar && \
    chown -R app:app /var/lib/oficina/registrar

COPY --from=build --chown=app:app /workspace/target/bot.jar /opt/oficina/registrar.jar

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom" \
    TZ=America/Sao_Paulo

WORKDIR /var/lib/oficina/registrar
USER 10001:10001

STOPSIGNAL SIGTERM
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["java", "-jar", "/opt/oficina/registrar.jar"]
