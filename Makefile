all:
	mvn clean package

run:
	java -jar target/agr_api-swarm.jar -Papp.properties

docker-build:
	docker build -t agrdocker/agr_api_server:develop .

push:
	docker push agrdocker/agr_api_server:develop

pull:
	docker pull agrdocker/agr_api_server:develop

bash:
	docker run -t -i agrdocker/agr_api_server:develop bash

docker-run:
	docker run -p 8080:8080 -t -i agrdocker/agr_api_server:develop

test:
	mvn test
