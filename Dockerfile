FROM eclipse-temurin:23-jdk-alpine AS builder
WORKDIR /app

COPY server/ ./server/

RUN chmod +x server/gradlew
RUN cd server && ./gradlew installDist

FROM eclipse-temurin:23-jre-alpine
WORKDIR /app

COPY --from=builder /app/server/build/install/server ./server

# Render injects $PORT; the server listens on it (HTTP + WebSocket).
EXPOSE 8080

CMD ["./server/bin/server"]
