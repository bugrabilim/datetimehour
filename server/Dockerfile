# Saat Tarih üyelik sunucusu (hesap.time.bumba.tr). Coolify: temel dizin server/, bu Dockerfile.
# Node sürümü sitedeki derleme imajıyla aynı ve sabit (standart, Altyapı kuralı); bağımlılıklar kilit dosyasından.
FROM node:22.23.4-alpine
ENV NODE_ENV=production
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev --ignore-scripts && npm cache clean --force
COPY src ./src
COPY i18n ./i18n
COPY data ./data
USER node
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s CMD wget -qO- http://127.0.0.1:8080/saglik >/dev/null || exit 1
CMD ["node", "src/index.mjs"]
