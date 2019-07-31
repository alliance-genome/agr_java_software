all:
	mvn -T 4 clean package

api:
	mvn clean package -pl agr_api -am

cacher:
	mvn clean package -pl agr_cacher -am

cacherq:
	mvn -q clean package -pl agr_cacher -am

cacherrun:
	java -jar agr_cacher/target/agr_cacher-jar-with-dependencies.jar

apiq:
	mvn -q clean package -pl agr_api -am

apirun:
	make -C agr_api run

apidebug:
	make -C agr_api debug

indexer:
	mvn clean package -pl agr_indexer -am

esutil:
	mvn clean package -pl agr_elasticsearch_util -am

core:
	mvn clean package -pl agr_java_core -am
