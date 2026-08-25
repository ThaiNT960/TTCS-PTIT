FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app


COPY pom.xml .
RUN mvn dependency:go-offline


COPY src ./src

# Build ra file .jar và bỏ qua việc chạy Test
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app


COPY --from=build /app/target/*.jar app.jar

# Mở cổng 8080
EXPOSE 8080

# Chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
