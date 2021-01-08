REG := 100225593120.dkr.ecr.us-east-1.amazonaws.com

registry-docker-login:
ifneq ($(shell echo ${REG} | egrep "ecr\..+\.amazonaws\.com"),)
	@$(eval DOCKER_LOGIN_CMD=aws)
ifneq (${AWS_PROFILE},)
	@$(eval DOCKER_LOGIN_CMD=${DOCKER_LOGIN_CMD} --profile ${AWS_PROFILE})
endif
	@$(eval DOCKER_LOGIN_CMD=${DOCKER_LOGIN_CMD} ecr get-login-password | docker login -u AWS --password-stdin https://${REG})
	${DOCKER_LOGIN_CMD}
endif

all: 
	mvn -q clean package

run:
	java -jar -Xmn6g -Xms6g target/agr_indexer-jar-with-dependencies.jar

test:
	mvn test

docker-build: pull
	docker build -t agrlocal/agr_indexer_run:develop --build-arg REG=${REG} .

docker-run: docker-build
	docker-compose up agr_indexer

pull: registry-docker-login
	docker pull ${REG}/agr_base_linux_env:develop

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

docker-pull-es: registry-docker-login
	docker pull ${REG}/agr_elasticsearch_env

docker-run-es: docker-pull-es
	docker run -p 9200:9200 -p 9300:9300 -e http.host=0.0.0.0 -e transport.host=0.0.0.0 -e xpack.security.enabled=false -e JAVA_OPTS="-Djava.net.preferIPv4Stack=true" ${REG}/agr_elasticsearch_env

docker-pull-neo: registry-docker-login
	docker pull ${REG}/agr_neo4j_data_image:build

docker-run-neo:
	docker run -p 127.0.0.1:7474:7474 -p 127.0.0.1:7687:7687 --env NEO4J_dbms_memory_heap_maxSize=8g ${REG}/agr_neo4j_data_image:build

