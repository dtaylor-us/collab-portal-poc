FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
COPY services/dpp-collaboration-api/pom.xml services/dpp-collaboration-api/pom.xml
COPY workers/planning-notification-worker/pom.xml workers/planning-notification-worker/pom.xml
RUN mvn -B -ntp -pl services/dpp-collaboration-api -am dependency:go-offline
COPY services/dpp-collaboration-api services/dpp-collaboration-api
COPY workflows workflows
RUN mvn -B -ntp -pl services/dpp-collaboration-api -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /workspace/services/dpp-collaboration-api/target/dpp-collaboration-api-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
