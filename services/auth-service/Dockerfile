# Stage 1 — BUILD
# We use a Maven image to compile and package the app into a jar
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first and download dependencies
# Docker caches this layer — if pom.xml didn't change, it won't re-download
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source code and build the jar
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2 — RUN
# Use a much smaller image just to run the jar
# The build image was ~500MB, this one is ~100MB
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 3001

ENTRYPOINT ["java", "-jar", "app.jar"]