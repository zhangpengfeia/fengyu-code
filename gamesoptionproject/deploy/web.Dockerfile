# Build with repository root as context: docker build -f gamesoptionproject/deploy/web.Dockerfile .
FROM node:22-bookworm-slim AS build
WORKDIR /src/gamesoptionproject
COPY agent-widget-sdk/ /src/agent-widget-sdk/
COPY gamesoptionproject/package.json gamesoptionproject/package-lock.json gamesoptionproject/.npmrc ./
RUN npm ci --prefer-offline --no-audit --no-fund --foreground-scripts --loglevel=info
COPY gamesoptionproject/index.html gamesoptionproject/tsconfig.json gamesoptionproject/vite.config.ts ./
COPY gamesoptionproject/src/ ./src/
COPY gamesoptionproject/public/ ./public/
RUN npm run build

FROM nginx:1.30.5-trixie
COPY gamesoptionproject/deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/gamesoptionproject/dist/ /usr/share/nginx/html/
ARG RELEASE_ID=local
RUN printf '%s\n' "$RELEASE_ID" > /usr/share/nginx/html/__release.txt
EXPOSE 8080
