all:
	mvn clean package

api:
	mvn clean package -pl agr_api -am

apirun: api
	make -C agr_api run

indexer:
	mvn clean package -pl agr_indexer -am

esutil:
	mvn clean package -pl agr_elasticsearch_util -am
