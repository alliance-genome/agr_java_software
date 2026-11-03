# agr_java_software

## Modules

- **agr_java_core** - Shared library (Neo4j entities, Elasticsearch utilities, configuration)
- **agr_api** - REST API server
- **agr_indexer** - Elasticsearch site index builder
- **agr_variant_indexer** - Elasticsearch variant index builder
- **agr_elasticsearch_util** - Elasticsearch utility tools
- **agr_api_tester** - API integration tests
- **agr_schema_validation** - LinkML schema validation
- **agr_intermine_data_extractor** - InterMine data extraction

## How to setup and build the API on a local system

1. Install docker, maven and java 11
2. Start up a Neo4j database image:
   `docker run -it -p 7474:7474 -p 7687:7687 --env NEO4J_AUTH=none -e NEO4J_dbms_memory_pagecache_size=8g -e NEO4J_dbms_memory_heap_maxSize=6g ${REG}/agr_neo4j_data_image:build`
3. Change directory into the agr_java_software directory
4. Build the API: `make api`
5. Change directory into agr_java_software/agr_api
6. Copy app.properties.defaults to app.properties and customize the configuration as needed. If developing locally with a local Neo4j instance, remove:
    * `NEO4J_HOST=build.alliancegenome.org`
7. Start app server: `make run` (for debug mode use `make debug` and remote connect to port 5045)
8. Call API endpoints, e.g. `http://localhost:8080/api/gene/MGI:109583/alleles`
