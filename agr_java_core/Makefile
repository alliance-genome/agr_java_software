REG := 100225593120.dkr.ecr.us-east-1.amazonaws.com

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
	mvn -q clean install

build:
	mvn -q clean install

test:
	mvn test

deploy:
	mvn deploy

pull: registry-docker-login
	docker pull ${REG}/agr_base_linux_env

docker-build: pull
	docker build -t agrlocal/agr_java_core_env --build-arg REG=${REG} .
