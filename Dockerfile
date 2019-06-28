FROM openjdk:8-alpine

COPY target/uberjar/guestbook2.jar /guestbook2/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/guestbook2/app.jar"]
