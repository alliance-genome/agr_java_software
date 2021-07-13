package org.alliancegenome.neo4j.repository;

import org.neo4j.ogm.model.Result;

public class DataExtractorRepository extends Neo4jRepository {

    public DataExtractorRepository() {
        super(null);
    }
    
    public Result getAllGenes() {
        
        String query = " MATCH p0=(s:Species)-[:FROM_SPECIES]-(g:Gene)";
        query += " OPTIONAL MATCH p1=(g:Gene)-[:ASSOCIATION]-(gl:GenomicLocation)";
        query += " OPTIONAL MATCH p2=(g:Gene)-[:ANNOTATED_TO]-(so:SOTerm)";
        query += " OPTIONAL MATCH p3=(g:Gene)-[:ALSO_KNOWN_AS]-(syn:Synonym)";
        query += " OPTIONAL MATCH p4=(g:Gene)-[:CROSS_REFERENCE]-(cr:CrossReference)";
        query += " RETURN g.primaryKey, g.modLocalId, collect(distinct replace(syn.name, ',', '')) as synonyms, collect(distinct cr.name) as crossrefs, g.name, g.symbol,  g.geneSynopsis, g.automatedGeneSynopsis, s.primaryKey, gl.chromosome, gl.start, gl.end, gl.strand, so.name";
            
        return queryForResult(query);

    }
    
    public Result getAllalleles() {

        String query = " MATCH p1=(:Species)<-[:FROM_SPECIES]-(a:Allele) ";
        //query += " where g.primaryKey = 'FB:FBgn0002121' AND a.primaryKey = 'FB:FBal0051412' ";
        //query += " where a.primaryKey in ['MGI:3795217','MGI:3712283','MGI:3843784','MGI:2158359'] ";
        query += " OPTIONAL MATCH gene=(a:Allele)-[:IS_ALLELE_OF]->(g:Gene)-[:FROM_SPECIES]-(q:Species)";
        query += " OPTIONAL MATCH vari=(a:Allele)<-[:VARIATION]-(variant:Variant)--(soTerm:SOTerm)";
        query += " OPTIONAL MATCH consequence=(:GeneLevelConsequence)<-[:ASSOCIATION]-(variant:Variant)";
        query += " OPTIONAL MATCH loc=(variant:Variant)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome)";
        query += " OPTIONAL MATCH variantPub=(variant:Variant)-[:ASSOCIATION]->(:Publication)";
        query += " OPTIONAL MATCH p2=(a:Allele)-[:ALSO_KNOWN_AS]->(synonym:Synonym)";
        query += " OPTIONAL MATCH crossRef=(a:Allele)-[:CROSS_REFERENCE]->(c:CrossReference)";
        query += " OPTIONAL MATCH disease=(a:Allele)<-[:IS_IMPLICATED_IN]-(doTerm:DOTerm)";
        query += " OPTIONAL MATCH pheno=(a:Allele)-[:HAS_PHENOTYPE]->(ph:Phenotype)";
        query += " OPTIONAL MATCH construct=(a:Allele)-[:CONTAINS]->(:Construct)";
        query += " RETURN p1, p2, vari, crossRef, disease, pheno, loc, consequence, gene, construct, variantPub ";
        
        return queryForResult(query);

    }
    
    
    
}
