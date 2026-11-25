# syntax=docker/dockerfile:1

# ---------- Stage 1: build the static site ----------
FROM node:24-alpine AS build
WORKDIR /app

# Install dependencies first (cached until package*.json changes).
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .
# Baked in at build time. Empty = call the same origin (/api), which nginx proxies.
ARG VITE_API_URL=""
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# ---------- Stage 2: serve with nginx ----------
FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=3 CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
