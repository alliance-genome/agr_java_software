all: 
	mvn -B clean install

test:
	mvn test

deploy:
	mvn deploy

pull:
	docker pull agrdocker/agr_java_env

docker-build: pull
	docker build -t agrdocker/agr_java_core_env .
