# Derleme yalnız repodakiyle çalışır; bağımlılık indirilmez.
FROM node:22-alpine AS build
WORKDIR /app
COPY . .
ARG SITE_ORIGIN
ARG UMAMI_ID
RUN node build.mjs

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
