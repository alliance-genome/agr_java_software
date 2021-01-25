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
	java -jar -Xmn6g -Xms6g target/agr_cacher-jar-with-dependencies.jar

test:
	mvn test

docker-build: pull
	docker build -t agrlocal/agr_variant_indexer_run:develop --build-arg REG=${REG} .

pull: registry-docker-login
	docker pull ${REG}/agr_base_linux_env:develop

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

docker-pull-es: registry-docker-login
	docker pull ${REG}/agr_infinispan_env

docker-run-es: docker-pull-es
	docker run -p 9200:9200 -p 9300:9300 ${REG}/agr_infinispan_env

docker-pull-neo: registry-docker-login
	docker pull ${REG}/agr_neo4j_data_image:build

docker-run-neo:
	docker run -p 127.0.0.1:7474:7474 -p 127.0.0.1:7687:7687 --env NEO4J_dbms_memory_heap_maxSize=8g ${REG}/agr_neo4j_data_image:build

