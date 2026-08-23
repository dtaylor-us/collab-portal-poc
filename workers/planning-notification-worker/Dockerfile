FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
COPY services/dpp-collaboration-api/pom.xml services/dpp-collaboration-api/pom.xml
COPY workers/planning-notification-worker/pom.xml workers/planning-notification-worker/pom.xml
RUN mvn -B -ntp -pl workers/planning-notification-worker -am dependency:go-offline
COPY workers/planning-notification-worker workers/planning-notification-worker
RUN mvn -B -ntp -pl workers/planning-notification-worker -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /workspace/workers/planning-notification-worker/target/planning-notification-worker-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
