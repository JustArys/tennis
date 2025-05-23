FROM amazoncorretto:21.0.4-alpine3.18
RUN mkdir -p "/app/uploads"
ARG JAR_FILE=target/*.jar
COPY   ./target/tennis.kz-0.0.1.jar app.jar
VOLUME /app/uploads
ENTRYPOINT ["java", "-jar", "/app.jar"]