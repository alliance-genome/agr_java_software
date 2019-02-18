all:
	mvn clean package

apirun: api
	make -C agr_api run

api:
	mvn clean package -pl agr_api -am

indexer:
	mvn clean package -pl agr_indexer -am

esutil:
	mvn clean package -pl agr_elasticsearch_util -am

submit:
	mvn clean package -pl agr_submission -am

core:
	mvn clean package -pl agr_java_core -am
