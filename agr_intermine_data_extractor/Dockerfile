FROM agrdocker/agr_base_linux_env:latest
  
WORKDIR /workdir/agr_intermine_data_extractor

ADD . .

RUN mvn -T 4 -B clean package

CMD java -jar target/agr_intermine_data_extractor-jar-with-dependencies.jar

