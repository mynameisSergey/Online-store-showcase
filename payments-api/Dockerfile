FROM openjdk:21-jdk-slim
LABEL authors="Sergey Iakovlev"
COPY build/libs/*.jar /payments-api.jar
ENTRYPOINT ["java", "-jar", "/payments-api.jar"]