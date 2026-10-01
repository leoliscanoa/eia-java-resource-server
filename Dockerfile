FROM maven:3.9.6-amazoncorretto-21 AS build
LABEL authors="leoliscanoa"
ARG GITHUB_USER
ARG GITHUB_TOKEN
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
ENV GITHUB_USER=${GITHUB_USER}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}
WORKDIR /opt/demo
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn dependency:go-offline || true
COPY ./src ./src
RUN mvn clean package -U -Dmaven.test.skip=true

FROM amazoncorretto:21-alpine
LABEL authors="leoliscanoa"
WORKDIR /opt/app
COPY --from=build /opt/demo/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
