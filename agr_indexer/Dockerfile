FROM agrdocker/agr_base_linux_env:latest

WORKDIR /workdir/agr_indexer

ADD . .

RUN mvn -T 4 -B clean package

CMD java -jar target/agr_indexer-jar-with-dependencies.jar
