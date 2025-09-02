FROM openjdk:21-jdk-slim
LABEL authors="Sergey Iakovlev"
COPY build/libs/*.jar /shop.jar
ENTRYPOINT ["java", "-jar", "/shop.jar"]