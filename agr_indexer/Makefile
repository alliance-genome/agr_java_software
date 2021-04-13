REG := 100225593120.dkr.ecr.us-east-1.amazonaws.com
DOCKER_PULL_TAG  := latest
DOCKER_BUILD_TAG := latest

registry-docker-login:
ifneq ($(shell echo ${REG} | egrep "ecr\..+\.amazonaws\.com"),)
	@$(eval DOCKER_LOGIN_CMD=docker run --rm -it -v ~/.aws:/root/.aws amazon/aws-cli)
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
	docker build -t agrlocal/agr_indexer_run:${DOCKER_BUILD_TAG} --build-arg REG=${REG} DOCKER_PULL_TAG=${DOCKER_PULL_TAG} .

docker-run: docker-build
	DOCKER_BUILD_TAG=${DOCKER_BUILD_TAG} docker-compose up agr_indexer

pull: registry-docker-login
	docker pull ${REG}/agr_base_linux_env:${DOCKER_PULL_TAG}

bash:
	DOCKER_BUILD_TAG=${DOCKER_BUILD_TAG} docker-compose up agr_indexer bash

startes:
	DOCKER_PULL_TAG=${DOCKER_PULL_TAG} docker-compose up -d agr_elasticsearch

stopes:
	docker-compose stop agr_elasticsearch

removees:
	docker-compose down -v



reload: stopes removees
	DOCKER_PULL_TAG=${DOCKER_PULL_TAG} docker-compose up agr_elasticsearch
	sleep 10
	DOCKER_BUILD_TAG=${DOCKER_BUILD_TAG} docker-compose up agr_indexer

docker-pull-es: registry-docker-login
	docker pull ${REG}/agr_elasticsearch_env:${DOCKER_PULL_TAG}

docker-run-es: docker-pull-es
	docker run -p 9200:9200 -p 9300:9300 -e http.host=0.0.0.0 -e transport.host=0.0.0.0 -e xpack.security.enabled=false -e JAVA_OPTS="-Djava.net.preferIPv4Stack=true" ${REG}/agr_elasticsearch_env:${DOCKER_PULL_TAG}

docker-pull-neo: registry-docker-login
	docker pull ${REG}/agr_neo4j_data_image:${DOCKER_PULL_TAG}

docker-run-neo:
	docker run -p 127.0.0.1:7474:7474 -p 127.0.0.1:7687:7687 -e NEO4J_dbms_memory_heap_max_size=12g -e NEO4J_dbms_memory_pagecache_size=12g ${REG}/agr_neo4j_data_image:${DOCKER_PULL_TAG}
