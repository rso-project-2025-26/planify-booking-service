FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn

COPY src ./src

RUN chmod +x ./mvnw && ./mvnw -q -e -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8086/actuator/health || exit 1

EXPOSE 8086 9095

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]