FROM eclipse-temurin:21-jre as runtime

ARG JAR_FILE=target/booking-service-1.0.0-SNAPSHOT.jar
WORKDIR /app

COPY ${JAR_FILE} app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8086 9095

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
