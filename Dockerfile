FROM openjdk:8-jdk-alpine
COPY build/libs/buns.jar buns.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/buns.jar"]