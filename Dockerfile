FROM gradle:9.1.0-jdk24 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

FROM eclipse-temurin:24-jre-noble
WORKDIR /app
RUN mkdir -p /app/logs
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
CMD ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]