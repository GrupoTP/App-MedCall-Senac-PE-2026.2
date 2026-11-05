# Imagens do Recife Saudável, construídas a partir desta pasta (recife-saudavel/):
#
#   docker build --target server -t rs-server .
#   docker build --target web --build-arg API_BASE_URL=https://recifesaudavel.grupotp.polyclub.com.br -t rs-web .
#
# O submódulo grupotp-kotlin-react-native precisa estar presente antes do build
# (git submodule update --init): o settings.gradle.kts inclui o build dele.

# ── Base de build: JDK 21 + Node.js 24 (Expo) ────────────────────────────────
FROM node:24-bookworm-slim AS node

FROM eclipse-temurin:21-jdk-noble AS build
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -s ../lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
    && ln -s ../lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx \
    && apt-get update \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"
WORKDIR /src
COPY . .
# O settings.gradle.kts só roda "git submodule update" quando o submódulo falta; aqui ele já veio no contexto.
RUN test -f grupotp-kotlin-react-native/settings.gradle.kts \
    || (echo "Submódulo grupotp-kotlin-react-native ausente: rode git submodule update --init" && exit 1)

# ── Servidor Ktor ─────────────────────────────────────────────────────────────
FROM build AS server-build
RUN sh ./gradlew --no-configuration-cache :server:installDist

FROM eclipse-temurin:21-jre-noble AS server
RUN useradd --system --uid 10001 --home-dir /app app
WORKDIR /app
COPY --from=server-build /src/server/build/install/server/ /app/
USER 10001
EXPOSE 8080
# JWT_SECRET é obrigatório. Banco: DATABASE_JDBC_URL, DATABASE_DRIVER, DATABASE_USER, DATABASE_PASSWORD.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["/app/bin/server"]

# ── PWA (export web do Expo) ──────────────────────────────────────────────────
FROM build AS web-build
# Endereço da API gravado no bundle. Mesmo host do PWA em produção.
ARG API_BASE_URL=""
ENV API_BASE_URL=${API_BASE_URL}
RUN sh ./gradlew --no-configuration-cache :client:expo:exportWeb

FROM nginx:1.30-alpine AS web
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=web-build /src/client/expo/dist/ /usr/share/nginx/html/
EXPOSE 8080
