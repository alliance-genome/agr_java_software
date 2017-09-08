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

docker-pull-es:
	docker pull agrdocker/agr_elasticsearch_env

docker-run-es:
	docker run -p 9200:9200 -p 9300:9300 -e http.host=0.0.0.0 -e transport.host=0.0.0.0 -e xpack.security.enabled=false -e JAVA_OPTS="-Djava.net.preferIPv4Stack=true" agrdocker/agr_elasticsearch_env

docker-pull-neo:
	docker pull agrdocker/agr_neo4j_qc_data_image:develop

docker-run-neo:
	docker run -p 7474:7474 -p 7687:7687 --env NEO4J_dbms_memory_heap_maxSize=8g agrdocker/agr_neo4j_qc_data_image:develop
