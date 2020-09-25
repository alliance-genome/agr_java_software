# agr_java_software


Trouble shooting
If the site is not responding:
Could be that wildfly (application server) is called before it is fully started: Solution: Restart wildfly

# How to setup and build the API on a local system

1. Install docker, maven and java 11
2. Start up a Neo4j database image (for BUILD / master repository): 
  `-docker run -it -p 7474:7474 -p 7687:7687 --env NEO4J_AUTH=none -e NEO4J_dbms_memory_pagecache_size=8g -e     NEO4J_dbms_memory_heap_maxSize=6g agrdocker/agr_neo4j_data_image:build`
3. Start up an infinispan docker image (for BUILD / master repository): `docker run -p 11222:11222 -it --name infinispan -e JAVA_OPTS="-Xmx16g" agrdocker/agr_infinispan_data_image:build`
This image contains the latest cache values done on a BUILD pipeline. You can use it as is. If you made changes in the caching logic you need to run the cacher 
4. Run the cache (unless you want to use the cache values as you can find them in the current infinispan docker image):
First build the cacher via `make cacher` then run the individual cachers, e.g. via
`java -Xmx16g -jar agr_cacher/target/agr_cacher-jar-with-dependencies.jar ExpressionCacher` when creating the expression annotations. The other entities are run with the following values (last command line option)
    DiseaseCacher
    AlleleCacher
    GenePhenotypeCacher
    InteractionCacher
    GeneOrthologCacher
    ExpressionCacher
    ModelCacher
    EcoCodeCacher
    ClosureCacher
    SiteMapCacher
3. Change directory into the agr_java_software directory
4. Build the API `make api`
5. Change directory into agr_java_software/agr_api
6. Copy app.properties.defaults to app.properties and customize the configuration as needed. Note: if you are developing on your local desktop and are running Neo and infinispan locally remove the following two property variables:
    `NEO4J_HOST=build.alliancegenome.org`
    `CACHE_HOST=build.alliancegenome.org`
    so the local instances are used for the API server.
7. Start app server (thorntail): `make run` If you want to be able to debug into the app server use `make debug` and remote connect to port 5045.
8. Now you can call API endpoints, e.g. `http://localhost:8080/api/gene/MGI:109583/alleles`
