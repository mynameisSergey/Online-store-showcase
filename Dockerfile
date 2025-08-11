FROM openjdk:21-jdk-slim
LABEL authors="Sergey Iakovlev"
COPY build/libs/*.jar /online-shop.jar
ENTRYPOINT ["java", "-jar", "/Online-store-showcase.jar"]