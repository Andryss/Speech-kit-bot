FROM eclipse-temurin:17-jre-alpine

ARG JAR_FILE=target/speech-bot*.jar

WORKDIR /opt/app

COPY ${JAR_FILE} speech-bot.jar

ENTRYPOINT ["java","-jar","speech-bot.jar"]
