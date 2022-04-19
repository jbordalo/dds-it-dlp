rm -rf build/
./gradlew build -x test
rm build/libs/spring-it-dlp-0.0.1-SNAPSHOT-plain.jar
docker build -t jc/dds-it-dlp .
