PROCS = -T 8
PACKAGE = clean package
#FLAGS = -DskipTests=true -ntp -Dorg.slf4j.simpleLogger.defaultLogLevel=WARN
FLAGS = -DskipTests=true

OPTS = $(PROCS) $(PACKAGE) $(FLAGS)

all:
	mvn $(OPTS)

%:
	mvn $(OPTS) -pl agr_$@ -am

cacherrun:
	java -jar agr_cacher/target/agr_cacher-jar-with-dependencies.jar

apirun:
	make -C agr_api run

apitesterrun:
	make -C agr_api_tester run

apidebug:
	make -C agr_api debug


searchtest:
	mvn -T 4 -Dit.test="AutocompteIntegrationSpec,QueryMatchIntegrationSpec,QueryRankIntegrationSpec,QueryTokenizationIntegrationSpec,RelatedDataServiceIntegrationSpec" -DfailIfNoTests=false verify
