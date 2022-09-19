package org.alliancegenome.neo4j.repository;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;

public class VariantRepository extends Neo4jRepository<Variant> {

	public VariantRepository() {
		super(Variant.class);
	}

	public List<Variant> getVariantsOfAllele(String id) {
		HashMap<String, String> map = new HashMap<>();
		String paramName = "alleleID";
		map.put(paramName, id);
		String query = "";
		query += " MATCH p1=(a:Allele)<-[:VARIATION]-(variant:Variant)--(soTerm:SOTerm) ";
		query += " WHERE a.primaryKey = {" + paramName + "}";
		query += " OPTIONAL MATCH synonyms=(variant:Variant)-[:ALSO_KNOWN_AS]-(:Synonym) ";
		query += " OPTIONAL MATCH notes=(variant:Variant)-[:ASSOCIATION]->(:Note) ";
		query += " OPTIONAL MATCH pubs=(variant:Variant)-[:ASSOCIATION]->(:Publication) ";
		query += " OPTIONAL MATCH crossRefs=(variant:Variant)-[:CROSS_REFERENCE]->(:CrossReference) ";
		query += " OPTIONAL MATCH consequence=(:GeneLevelConsequence)<-[:ASSOCIATION]-(variant:Variant)";
		query += " OPTIONAL MATCH transcripts=(:GenomicLocation)--(variant:Variant)-[:ASSOCIATION]-(t:Transcript)<-[:TRANSCRIPT_TYPE]-(:SOTerm)";
		query += " OPTIONAL MATCH transcriptConsequence=(variant:Variant)--(tlc:TranscriptLevelConsequence)<-[:ASSOCIATION]-(t:Transcript)--(variant:Variant)";
		query += " OPTIONAL MATCH loc=(variant:Variant)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome)";
		query += " OPTIONAL MATCH p2=(variant:Variant)<-[:COMPUTED_GENE]-(:Gene)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome)";
		query += " RETURN p1, p2, loc, consequence, synonyms, transcripts, transcriptConsequence, notes, crossRefs, pubs ";

		Iterable<Variant> alleles = query(query, map);
		return StreamSupport.stream(alleles.spliterator(), false)
				.collect(Collectors.toList());

	}

	public Variant getVariant(String variantID) {
		HashMap<String, String> map = new HashMap<>();
		String paramName = "variantID";
		map.put(paramName, variantID);
		String query = "";
		query += " MATCH p1=(t:Transcript)-[:ASSOCIATION]->(variant:Variant)--(soTerm:SOTerm) ";
		query += " WHERE variant.primaryKey = {" + paramName + "}";
		query += " OPTIONAL MATCH consequence=(:GenomicLocation)--(variant:Variant)-[:ASSOCIATION]->(:TranscriptLevelConsequence)" +
				"<-[:ASSOCIATION]-(t:Transcript)<-[:TRANSCRIPT_TYPE]-(:SOTerm)";
		query += " OPTIONAL MATCH gene=(t:Transcript)-[:TRANSCRIPT]-(:Gene)--(:GenomicLocation)--(:Chromosome)";
		query += " OPTIONAL MATCH transcriptLocation=(t:Transcript)-[:ASSOCIATION]-(:GenomicLocation)--(:Chromosome)";
		query += " OPTIONAL MATCH exons=(:GenomicLocation)--(:Exon)-[:EXON]->(t:Transcript)";
		query += " RETURN p1, consequence, gene, exons, transcriptLocation ";

		Iterable<Variant> variants = query(query, map);
		for (Variant a : variants) {
			if (a.getPrimaryKey().equals(variantID)) {
				return a;
			}
		}
		return null;
	}

	public List<Allele> getAllelesOfVariant(String variantID) {
		String query = "";
		query += " MATCH p1=(a:Allele)<-[:VARIATION]-(variant:Variant)--(soTerm:SOTerm) ";
		query += " WHERE variant.primaryKey = '" + variantID + "'";
		query += " RETURN p1  ";

		Iterable<Allele> alleles = query(Allele.class, query);
		return StreamSupport.stream(alleles.spliterator(), false)
				.collect(Collectors.toList());
	}

	public GenomeLocation getGenomeLocation(String geneID) {
		String query = "";
		query += " MATCH p1=(gene:Gene)--(location:GenomicLocation)--(:Chromosome) ";
		query += " WHERE gene.primaryKey = '" + geneID + "'";
		query += " RETURN p1  ";

		Iterable<GenomeLocation> locations = query(GenomeLocation.class, query);
		if (locations == null)
			return null;
		// return first location
		return locations.iterator().next();
	}
}
