# agr_java_software


Trouble shooting
If the site is not responding:
Could be that wildfly (application server) is called before it is fully started: Solution: Restart wildfly

# How to setup and build the API on a local system

1. Install docker, maven and java 8
2. Start up a Neo4j database image (for BUILD / master repository): 
  `-docker run -it -p 7474:7474 -p 7687:7687 --env NEO4J_AUTH=none -e NEO4J_dbms_memory_pagecache_size=8g -e     NEO4J_dbms_memory_heap_maxSize=6g agrdocker/agr_neo4j_data_image:build`
3. Change directory into the agr_java_software directory
4. Build the API `make api`
5. Change directory into agr_java_software/agr_api
6. Start app server (thorntail): `make run`
7. Now you can call API endpoints, e.g. `http://localhost:8080/api/gene/MGI:109448/alleles`
