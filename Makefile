all:
	mvn clean package

run:
	java -jar target/agr_api-swarm.jar -Papp.properties
