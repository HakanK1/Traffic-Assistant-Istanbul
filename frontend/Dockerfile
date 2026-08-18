# Stage 1: Build Process
FROM node:22-alpine AS build
WORKDIR /app

# copying the package files and installing dependencies
COPY package*.json ./
RUN npm install

# copying all source code and compiling it for production
COPY . .
RUN npm run build --configuration=production

# Stage 2: Serving Process
FROM nginx:alpine

# copying our compiled files into Nginx’s default web-serving directory.
# In Angular 17+ versions, the compiled files are usually located in the dist/<project-name>/browser folder.
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

# Nginx runs on port 80 by default
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]