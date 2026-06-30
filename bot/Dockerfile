# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="Oficina Bot" \
      org.opencontainers.image.description="Runtime image for the Oficina Discord bot"

RUN apk add --no-cache dumb-init tzdata && \
    addgroup -S -g 10001 app && \
    adduser -S -D -H -u 10001 -h /var/lib/oficina/bot -s /sbin/nologin -G app app && \
    mkdir -p /opt/oficina /var/lib/oficina/bot && \
    chown -R app:app /var/lib/oficina/bot

COPY --from=build --chown=app:app /workspace/target/bot.jar /opt/oficina/bot.jar

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom" \
    TZ=America/Sao_Paulo

WORKDIR /var/lib/oficina/bot
USER 10001:10001

STOPSIGNAL SIGTERM
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["java", "-jar", "/opt/oficina/bot.jar"]
