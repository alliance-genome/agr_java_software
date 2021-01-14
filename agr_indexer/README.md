# Indexing Tips
## Basic Steps for Indexing Locally
1. Start up Neo4j 
````      
% cd agr_indexer && make docker-run-neo
````
2. start elasticsearch (container or local install)
3. Build the entire project from the top level 
````
% make all
````
4. Run the indexer, this will index all site_index documents, putting them into a site_index_[timestamp] index 
````
% cd agr_indexer && java -jar -Xmn6g -Xms6g target/agr_indexer-jar-with-dependencies.jar
````
5. Add an alias so that this new timestamped index can be accessed simply as site_index 
````
% curl -XPUT "http://localhost:9200/site_index_*/_alias/site_index"
````

## Updating an existing index
If the mappings & settings haven't changed, you can save time by making partial updates of an existing index
by adding `-DKEEPINDEX=true` to the indexing command. To re-index only disease records the command would be 
````
java -jar -Xmn6g -Xms6g -DKEEPINDEX="true" target/agr_indexer-jar-with-dependencies.jar disease
````

## Specifying a species for a quicker turnaround
Indexing all genes can take a little while, so there is an option to limit indexing to only one species with 
`-DSPECIES=<Species name>`. This flag can be used for genes, alleles and models. To keep the index and only 
update/add zebrafish alleles the command would be
````
java -jar -Xmn6g -Xms6g -DKEEPINDEX="true" -DSPECIES="Danio rerio" target/agr_indexer-jar-with-dependencies.jar allele
````

> Not sure which categories you can specify? Check in [IndexerConfig.java](https://github.com/alliance-genome/agr_java_software/blob/master/agr_indexer/src/main/java/org/alliancegenome/indexer/config/IndexerConfig.java)

> Not sure how to spell the species names? Check the species facet in production search, or here's the r**ae**lly fun ones: 
* `Caenorhabditis elegans` 
* `Saccharomyces cerevisiae`
