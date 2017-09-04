FROM agrdocker/agr_java_env:latest

WORKDIR /workdir/agr_api

ADD . .
RUN mvn clean package

CMD java -jar target/agr_api-swarm.jar
