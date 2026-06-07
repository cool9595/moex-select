FROM node:20-alpine
WORKDIR /app

# Слой зависимостей кэшируется отдельно
COPY package.json package-lock.json ./
RUN npm ci

# Исходники
COPY . .

ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL

EXPOSE 5173
# --host 0.0.0.0 делает dev-сервер доступным с хоста (порт проброшен в compose).
# Флаг переопределяет host из vite.config.ts, не меняя сам файл.
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173"]
