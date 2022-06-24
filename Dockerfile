FROM gradle:7.4.2-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build -x test --no-daemon

FROM openjdk:17
ENV TLS_KEYSTORE_PASSWORD=dds-it-dlp
ENV STORAGE_PATH=mnt/vol/
ENV SERVER_KEYSTORE=config/keystores/serverKeystore SERVER_KEYSTORE_ALIAS=appservice SERVER_KEYSTORE_PW=ddsdds
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
COPY config config
COPY config/hosts-docker.config config/hosts.config
RUN rm config/currentView; exit 0
ENTRYPOINT ["java","-jar","/app.jar"]
