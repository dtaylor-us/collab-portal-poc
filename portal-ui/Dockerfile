FROM nginx:1.27-alpine
COPY portal-ui/nginx.conf /etc/nginx/conf.d/default.conf
COPY portal-ui/index.html /usr/share/nginx/html/index.html
COPY portal-ui/styles.css /usr/share/nginx/html/styles.css
COPY portal-ui/app.js /usr/share/nginx/html/app.js
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --retries=5 CMD wget -q --spider http://127.0.0.1:8080/health || exit 1
