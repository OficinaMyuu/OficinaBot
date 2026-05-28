FROM golang:1.24.4-alpine3.22 AS build

WORKDIR /app

# Cache Go modules dependencies layer
COPY cmd/go.mod cmd/go.sum ./
RUN go mod download

# Copy source files
COPY cmd/ ./

# Build optimized statically-linked binary with cache mounts and compiler optimizations (-ldflags="-s -w")
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o server ./api/main.go

# Final/Run Stage
FROM debian:bookworm-slim

# Upgrade to Debian Bookworm (removes old CVEs) and install Chromium, ffmpeg, yt-dlp, and dumb-init
RUN apt-get update && \
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

# Create a non-privileged app user and group for security hardening (no root container execution)
RUN groupadd -r appgroup && useradd -r -g appgroup -u 10001 appuser

# Set up working directory and copy compiled application
WORKDIR /app
COPY --from=build /app/server /app/server
COPY static/ /app/static/

# Set up database persistent volume path with correct ownership for SQLite WAL writes
RUN mkdir -p /app/data && chown -R appuser:appgroup /app

# Shift execution context to the non-root app user
USER appuser

# Set env vars
ENV PATH="/app:/usr/local/bin:${PATH}"
ENV TMPDIR="/tmp"

EXPOSE 8080

# Use dumb-init to handle system signals and zombie processes cleanly inside containers
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["./server"]