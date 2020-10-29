package org.alliancegenome.neo4j.repository;

import org.neo4j.ogm.model.Result;

public class DataExtractorRepository extends Neo4jRepository {

    public DataExtractorRepository() {
        super(null);
    }

    public Result getAllGenes() {

        String query = " MATCH p1=(s:Species)-[:FROM_SPECIES]-(g:Gene)-[:ASSOCIATION]-(gl:GenomicLocation)";
        query += " OPTIONAL MATCH p2=(g:Gene)-[:ANNOTATED_TO]-(so:SOTerm)";
        query += " RETURN g.primaryKey, g.name, g.description, s.primaryKey, gl.chromosome, gl.start, gl.end, gl.strand, so.name";
        
        return queryForResult(query);

    }
}
