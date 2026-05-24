FROM golang:1.24.4-alpine3.22 AS build

WORKDIR /app

COPY cmd/go.mod cmd/go.sum ./

RUN go mod download

COPY cmd/ ./

RUN CGO_ENABLED=0 GOOS=linux go build -o server ./api/main.go

# Final/Run Stage
FROM debian:bullseye-slim

# Install Chromium, ffmpeg, yt-dlp
RUN apt-get update && \
    apt-get install -y \
        chromium \
        ffmpeg \
        curl \
        ca-certificates \
        fonts-liberation \
        --no-install-recommends && \
    curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp && \
    chmod a+rx /usr/local/bin/yt-dlp && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy app binary and static files
WORKDIR /app
COPY --from=build /app/server /app/server
COPY static/ /app/static/

# Set env vars
ENV PATH="/app:/usr/local/bin:${PATH}"
ENV TMPDIR="/tmp"

EXPOSE 8080
ENTRYPOINT ["./server"]