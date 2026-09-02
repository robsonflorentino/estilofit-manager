# ── Estágio 1: build dos estáticos (Vite) ────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Em produção o frontend e a API ficam na mesma origem (nginx faz proxy de /api).
# Por isso a base da API é relativa: /api/v1
ENV VITE_API_BASE_URL=/api/v1
RUN npm run build

# ── Estágio 2: nginx servindo os estáticos + proxy para a API ────────────────
FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
