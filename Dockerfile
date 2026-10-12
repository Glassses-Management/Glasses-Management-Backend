# ---- Build stage ----
# Uses the Maven that ships with the image rather than the ./mvnw wrapper, which
# would otherwise have to download a second distribution inside the build.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -B -q package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Bangkok \
    SPRING_PROFILES_ACTIVE=production
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
