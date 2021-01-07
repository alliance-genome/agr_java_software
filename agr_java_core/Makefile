all: 
	mvn -q clean install

build:
	mvn -q clean install

test:
	mvn test

deploy:
	mvn deploy

pull:
	docker pull agrdocker/agr_base_linux_env

docker-build: pull
	docker build -t agrdocker/agr_java_core_env .
