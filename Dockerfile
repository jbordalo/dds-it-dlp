FROM openjdk:17
ENV KEYSTORE_PASSWORD=dds-it-dlp
COPY build/libs/*.jar app.jar
COPY config config
COPY config/hosts-docker.config config/hosts.config
ENTRYPOINT ["java","-jar","/app.jar"]
