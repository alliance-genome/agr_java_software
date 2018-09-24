all:
	mvn clean package

api:
	mvn clean package -pl agr_api -am

indexer:
	mvn clean package -pl agr_indexer -am
