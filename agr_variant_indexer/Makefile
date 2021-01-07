all: 
	mvn -q clean package

run:
	java -jar -Xmn6g -Xms6g target/agr_cacher-jar-with-dependencies.jar

test:
	mvn test

docker-build: pull
	docker build -t agrdocker/agr_cacher_run:develop .

docker-run: docker-build
	docker-compose up agr_cache

pull:
	docker pull agrdocker/agr_base_linux_env:develop

bash:
	docker-compose up agr_cacher bash

startes:
	docker-compose up -d agr_infinispan

stopes:
	docker-compose stop agr_infinispan

removees:
	docker-compose down -v



reload: stopes removees
	docker-compose up agr_infinispan
	sleep 10
	docker-compose up agr_cacher

docker-pull-es:
	docker pull agrdocker/agr_infinispan_env

docker-run-es: docker-pull-es
	docker run -p 9200:9200 -p 9300:9300 agrdocker/agr_infinispan_env

docker-pull-neo:
	docker pull agrdocker/agr_neo4j_data_image:build

docker-run-neo:
	docker run -p 127.0.0.1:7474:7474 -p 127.0.0.1:7687:7687 --env NEO4J_dbms_memory_heap_maxSize=8g agrdocker/agr_neo4j_data_image:build

