package org.alliancegenome.neo4j.repository;

import org.alliancegenome.neo4j.entity.node.Gene;
import org.neo4j.ogm.model.Result;

public class GeneDataExtractorRepository extends Neo4jRepository<Gene> {

    public GeneDataExtractorRepository() {
        super(Gene.class);
    }

    public Result getAllGenes() {

        String query = " MATCH p1=(s:Species)-[:FROM_SPECIES]-(g:Gene)-[:ASSOCIATION]-(gl:GenomicLocation)";
        query += " OPTIONAL MATCH p2=(g:Gene)-[:ANNOTATED_TO]-(so:SOTerm)";
        query += " RETURN g.primaryKey, g.name, s.primaryKey, gl.chromosome, gl.start, gl.end, gl.strand, so.name";
        
        return queryForResult(query);

    }
}
