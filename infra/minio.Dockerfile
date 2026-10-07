# Build the pinned upstream source: the historical public MinIO images are unavailable.
FROM golang:1.24-bookworm AS server-build
ADD https://github.com/minio/minio/archive/refs/tags/RELEASE.2025-09-07T16-13-09Z.tar.gz /source.tar.gz
RUN mkdir /source && tar -xzf /source.tar.gz -C /source --strip-components=1
WORKDIR /source
RUN CGO_ENABLED=0 go build -trimpath -o /minio .

FROM golang:1.24-bookworm AS client-build
ADD https://github.com/minio/mc/archive/refs/tags/RELEASE.2025-08-13T08-35-41Z.tar.gz /source.tar.gz
RUN mkdir /source && tar -xzf /source.tar.gz -C /source --strip-components=1
WORKDIR /source
RUN CGO_ENABLED=0 go build -trimpath -o /mc .

FROM debian:bookworm-slim AS base
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && rm -rf /var/lib/apt/lists/*

FROM base AS client
COPY --from=client-build /mc /usr/local/bin/mc

FROM base AS server
COPY --from=server-build /minio /usr/local/bin/minio
RUN mkdir /data && chown 10001:10001 /data
USER 10001:10001
ENTRYPOINT ["minio"]
