FROM golang:1.24.4-alpine3.22 AS build

WORKDIR /app

RUN apk update && apk upgrade --no-cache

COPY cmd/go.mod cmd/go.sum ./
RUN go mod download

COPY cmd/ ./

RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o server ./api/main.go

FROM debian:bookworm-slim

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
        chromium \
        ffmpeg \
        curl \
        ca-certificates \
        fonts-liberation \
        dumb-init && \
    curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp && \
    chmod a+rx /usr/local/bin/yt-dlp && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd -r appgroup && useradd -r -g appgroup -u 10001 appuser

WORKDIR /app
COPY --from=build /app/server /app/server
COPY static/ /app/static/

RUN mkdir -p /app/data && chown -R appuser:appgroup /app

USER appuser

ENV PATH="/app:/usr/local/bin:${PATH}"
ENV TMPDIR="/tmp"

EXPOSE 8080

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["./server"]