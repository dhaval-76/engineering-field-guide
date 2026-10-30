# syntax=docker/dockerfile:1
FROM node:22-slim AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev                       # exact versions from the lockfile, no dev deps

FROM node:22-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build                           # e.g. TypeScript → dist/

FROM node:22-slim
ENV NODE_ENV=production
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY package.json ./
USER node                                   # the image ships a non-root "node" user
EXPOSE 3000
CMD ["node", "dist/server.js"]              # exec form; handle SIGTERM in the app to drain requests
