FROM agrdocker/agr_java_env:latest

WORKDIR /workdir/agr_indexer

ADD . .

RUN mvn -B clean package

CMD java -jar target/agr_indexer-jar-with-dependencies.jar
