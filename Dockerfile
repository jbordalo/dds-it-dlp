FROM gradle:7.4.2-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build -x test --no-daemon

FROM openjdk:17
ENV TLS_KEYSTORE_PASSWORD=dds-it-dlp
ENV SERVER_KEYSTORE=config/keystores/serverKeystore SERVER_KEYSTORE_ALIAS=appservice SERVER_KEYSTORE_PW=ddsdds
ENV ENDORSERS_KEYSTORE=config/keystores/publicEndorsersKeystore ENDORSERS_KEYSTORE_ALIAS=endorser ENDORSERS_KEYSTORE_PW=ddsdds
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
COPY config config
COPY config/hosts-cloud.config config/hosts.config
COPY config/config-cloud.properties config/config.properties
RUN rm config/currentView; exit 0
ENTRYPOINT ["java","-jar","/app.jar"]
