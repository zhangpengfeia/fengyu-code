# Repository root is the build context: the shared widget SDK is a local dependency.
FROM node:22-bookworm-slim AS build
WORKDIR /src/student-web
COPY agent-widget-sdk/ /src/agent-widget-sdk/
COPY student-web/package*.json ./
RUN npm ci --no-audit --no-fund
COPY student-web/ ./
ARG VITE_AGENT_TOKEN_ENDPOINT=
ENV VITE_BASE_PATH=/student/ \
    VITE_EDUCATION_API_BASE=/prod-api \
    VITE_AGENT_API_BASE=/student/api \
    VITE_AGENT_TOKEN_ENDPOINT=${VITE_AGENT_TOKEN_ENDPOINT}
RUN npm run build

FROM nginx:1.28-alpine
RUN apk add --no-cache curl
COPY student-web/deploy/nginx.agent.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/student-web/dist/ /usr/share/nginx/html/student/
ARG RELEASE_ID=development
RUN printf '%s\n' "$RELEASE_ID" > /usr/share/nginx/html/student/__release.txt
EXPOSE 8080
