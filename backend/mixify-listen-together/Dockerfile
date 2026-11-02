FROM golang:1.22-alpine AS builder

WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go build -o server .

FROM alpine:latest
WORKDIR /root/
COPY --from=builder /app/server .

# Run as non-root user for security
RUN adduser -D myuser
USER myuser

EXPOSE 8080
CMD ["./server"]