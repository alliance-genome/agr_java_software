all: 
	mvn clean package

test:
	mvn test

deploy:
	mvn deploy

pull:
	docker pull agrdocker/agr_java_env

docker-build: pull
	docker build -t agrdocker/agr_java_shared_env .
