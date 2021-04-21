FROM agrdocker/agr_base_linux_env:latest
  
WORKDIR /workdir/agr_api_tester

ADD . .

RUN mvn -T 4 -B clean package

CMD java -jar target/agr_api_tester-jar-with-dependencies.jar

