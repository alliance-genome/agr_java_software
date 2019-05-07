all:
	mvn clean package

api:
	mvn clean package -pl agr_api -am

apiq:
	mvn -q clean package -pl agr_api -am

apirun: api
	make -C agr_api run

api:
	mvn clean package -pl agr_api -am

indexer:
	mvn clean package -pl agr_indexer -am

esutil:
	mvn clean package -pl agr_elasticsearch_util -am

core:
	mvn clean package -pl agr_java_core -am
