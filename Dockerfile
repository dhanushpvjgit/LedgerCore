# ---- build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn --batch-mode -q dependency:go-offline
COPY src ./src
RUN mvn --batch-mode -q clean package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
RUN useradd --system --create-home --uid 10001 ledgercore
WORKDIR /app
COPY --from=build /workspace/target/ledgercore.jar app.jar
USER ledgercore
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
