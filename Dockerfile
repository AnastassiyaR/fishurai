FROM eclipse-temurin:23-jdk-alpine AS builder
WORKDIR /app

COPY server/ ./server/

RUN chmod +x server/gradlew
RUN cd server && ./gradlew installDist

FROM eclipse-temurin:23-jre-alpine
WORKDIR /app

COPY --from=builder /app/server/build/install/server ./server

EXPOSE 8087 8088

CMD ["./server/bin/server"]
