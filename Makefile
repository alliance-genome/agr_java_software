all:
	mvn clean package -Pwildfly-swarm

run:
	java -jar target/agr_api-swarm.jar
