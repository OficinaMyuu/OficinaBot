# syntax=docker/dockerfile:1.7

FROM golang:1.25-alpine3.22 AS build

WORKDIR /workspace

RUN apk update && apk upgrade --no-cache

COPY go.mod go.sum ./
RUN --mount=type=cache,target=/go/pkg/mod \
    go mod download

COPY . .
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /out/registrar ./cmd/registrar

FROM alpine:3.22

LABEL org.opencontainers.image.title="Oficina Registrar" \
      org.opencontainers.image.description="Runtime image for the Oficina registration Discord service"

RUN apk add --no-cache ca-certificates dumb-init tzdata && \
    addgroup -S -g 10001 app && \
    adduser -S -D -H -u 10001 -h /var/lib/oficina/registrar -s /sbin/nologin -G app app && \
    mkdir -p /opt/oficina /var/lib/oficina/registrar && \
    chown -R app:app /var/lib/oficina/registrar

COPY --from=build --chown=app:app /out/registrar /opt/oficina/registrar

ENV TZ=America/Sao_Paulo

WORKDIR /var/lib/oficina/registrar
USER 10001:10001

STOPSIGNAL SIGTERM
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["/opt/oficina/registrar"]
