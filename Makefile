all:
	mvn clean package

run:
	java -jar target/agr_api-swarm.jar -Papp.properties

docker-build:
	docker build -t agrdocker/agr_api_server:develop .

docker-run-command:
	java -jar target/agr_api-swarm.jar

push:
	docker push agrdocker/agr_api_server:develop

pull:
	docker pull agrdocker/agr_api_server:develop

bash:
	docker run -t -i agrdocker/agr_api_server:develop bash

docker-run:
	docker run -p 8080:8080 -t -i agrdocker/agr_api_server:develop

docker-pull-es:
	docker pull agrdocker/agr_es_data_image:develop

docker-run-es:
	docker run -p 9200:9200 -p 9300:9300 -e "http.host=0.0.0.0" -e "transport.host=0.0.0.0" agrdocker/agr_es_data_image:develop

test:
	mvn test
