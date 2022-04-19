FROM gradle:7.4.2-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build -x test --no-daemon

FROM openjdk:17
ENV KEYSTORE_PASSWORD=dds-it-dlp
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
COPY config config
COPY config/hosts-docker.config config/hosts.config
ENTRYPOINT ["java","-jar","/app.jar"]
