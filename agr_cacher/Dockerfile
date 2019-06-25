FROM agrdocker/agr_base_linux_env:latest
  
WORKDIR /workdir/agr_cacher

ADD . .

RUN mvn -B clean package

CMD java -jar target/agr_cacher-jar-with-dependencies.jar

