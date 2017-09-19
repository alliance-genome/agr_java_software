all:
	mvn clean package

run:
	java -jar target/agr_api-swarm.jar -Papp.properties

debug:
	java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5045 -jar target/agr_api-swarm.jar -Papp.properties

docker-build:
	docker build -t agrdocker/agr_api_server .

docker-run-command:
	java -jar target/agr_api-swarm.jar

push:
	docker push agrdocker/agr_api_server

pull:
	docker pull agrdocker/agr_api_server

bash:
	docker run -t -i agrdocker/agr_api_server bash

docker-run:
	docker run -p 8080:8080 -t -i agrdocker/agr_api_server

docker-pull-es:
	docker pull agrdocker/agr_es_data_image

docker-run-es:
	docker run -p 9200:9200 -p 9300:9300 -e "http.host=0.0.0.0" -e "transport.host=0.0.0.0" agrdocker/agr_es_data_image

docker-run-es-detached:
	docker run -d -p 9200:9200 -p 9300:9300 -e "http.host=0.0.0.0" -e "transport.host=0.0.0.0" agrdocker/agr_es_data_image:develop

test:
	mvn test
