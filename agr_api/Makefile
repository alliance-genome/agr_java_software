all:
	mvn -q -T 8 clean package -Dquarkus.package.type=uber-jar -ntp

run:
	java -jar target/agr_api-thorntail.jar -Xms4g -Xmx4g -Papp.properties

run-dev:
	java -jar target/agr_api-thorntail.jar -Papp.properties -DES_INDEX=site_index_dev

debug:
	java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5045 -jar target/agr_api-thorntail.jar -Papp.properties

docker-run-command:
	java -jar target/agr_api-swarm.jar

test:
	mvn test

verify:
	mvn verify
