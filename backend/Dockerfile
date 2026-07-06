FROM golang:1.25-alpine3.22 AS build

WORKDIR /app

RUN apk update && apk upgrade --no-cache

COPY backend/cmd/go.mod backend/cmd/go.sum ./backend/cmd/

WORKDIR /app/backend/cmd
RUN go mod download

COPY backend/cmd/ ./

RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o /app/server ./api/main.go

RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    PLAYWRIGHT_DRIVER_PATH=/tmp/playwright-driver \
    printf 'package main\nimport "github.com/playwright-community/playwright-go"\nfunc main() { if err := playwright.Install(&playwright.RunOptions{DriverDirectory: "/tmp/playwright-driver", SkipInstallBrowsers: true}); err != nil { panic(err) } }\n' > /tmp/install-playwright-driver.go && \
    go run /tmp/install-playwright-driver.go && \
    rm /tmp/install-playwright-driver.go

FROM debian:bookworm-slim

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
        chromium \
        curl \
        ca-certificates \
        fonts-liberation \
        dumb-init && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd -r appgroup && \
    useradd -r -g appgroup -u 10001 -m -d /home/appuser appuser && \
    mkdir -p /var/lib/oficina/backend/playwright-driver /var/cache/oficina/backend

WORKDIR /app
COPY --from=build /app/server /app/server
COPY --from=build /tmp/playwright-driver /var/lib/oficina/backend/playwright-driver
COPY backend/static/ /app/static/

RUN chown -R appuser:appgroup /app /home/appuser /var/lib/oficina/backend /var/cache/oficina/backend

USER appuser

ENV PATH="/app:/usr/local/bin:${PATH}"
ENV TMPDIR="/tmp"
ENV HOME="/home/appuser"
ENV XDG_CACHE_HOME="/var/cache/oficina/backend"
ENV PLAYWRIGHT_DRIVER_PATH="/var/lib/oficina/backend/playwright-driver"
ENV PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH="/usr/bin/chromium"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -fsS http://localhost:8080/health || exit 1

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["./server"]
