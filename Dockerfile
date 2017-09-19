FROM agrdocker/agr_java_env

WORKDIR /workdir/agr_api

EXPOSE 8080

ADD . .
RUN mvn clean package

CMD java -jar target/agr_api-swarm.jar
