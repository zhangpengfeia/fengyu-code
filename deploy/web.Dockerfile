FROM node:22-bookworm-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY index.html tsconfig.json vite.config.ts ./
COPY src/ ./src/
COPY public/ ./public/
RUN npm run build

FROM nginx:1.30.5-trixie
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/ /usr/share/nginx/html/
ARG RELEASE_ID=local
RUN printf '%s\n' "$RELEASE_ID" > /usr/share/nginx/html/__release.txt
EXPOSE 8080
