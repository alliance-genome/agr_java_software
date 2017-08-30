all: 
	mvn clean package

run:
	java -jar target/agr_indexer-jar-with-dependencies.jar

test:
	mvn test

docker-build: pull
	docker build -t agrdocker/agr_indexer_run:develop .

docker-run: docker-build
	docker-compose up agr_indexer

pull:
	docker pull agrdocker/agr_java_env:develop

bash:
	docker-compose up agr_indexer bash

startes:
	docker-compose up -d agr_elasticsearch

stopes:
	docker-compose stop agr_elasticsearch

removees:
	docker-compose down -v



reload: stopes removees
	docker-compose up agr_elasticsearch
	sleep 10
	docker-compose up agr_indexer
