FROM golang:1.24.4-alpine3.22 AS build

WORKDIR /app

RUN apk update && apk upgrade --no-cache

COPY database/go.mod database/go.sum ./database/
COPY backend/cmd/go.mod backend/cmd/go.sum ./backend/cmd/

WORKDIR /app/backend/cmd
RUN go mod download

COPY database/ /app/database/
COPY backend/cmd/ ./

RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o /app/server ./api/main.go

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
COPY backend/static/ /app/static/

RUN chown -R appuser:appgroup /app

USER appuser

ENV PATH="/app:/usr/local/bin:${PATH}"
ENV TMPDIR="/tmp"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -fsS http://localhost:8080/health || exit 1

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["./server"]
