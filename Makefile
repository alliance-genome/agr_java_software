all:
	mvn clean package

run:
	java -jar target/agr_api-swarm.jar -Papp.properties


docker-build:
	docker build -t agrdocker/agr_api_server .

push:
	docker push agrdocker/agr_api_server

pull:
	docker pull agrdocker/agr_api_server

bash:
	docker run -t -i agrdocker/agr_api_server bash

docker-run:
	docker run -p 8080:8080 -t -i agrdocker/agr_api_server
