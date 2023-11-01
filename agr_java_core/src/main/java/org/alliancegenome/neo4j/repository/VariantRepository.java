package org.alliancegenome.neo4j.repository;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Variant;

public class VariantRepository extends Neo4jRepository<Variant> {

	public VariantRepository() {
		super(Variant.class);
	}

	public List<Variant> getVariantsOfAllele(String id) {
		HashMap<String, String> map = new HashMap<>();
		String paramName = "alleleID";
		map.put(paramName, id);
		String query = "";
		query += " MATCH p1=(a:Allele)<-[:VARIATION]-(variant:Variant)--(soTerm:SOTerm) WHERE a.primaryKey = $" + paramName;
		query += " WITH a, variant, p1";
		query += " OPTIONAL MATCH synonyms=(variant:Variant)-[:ALSO_KNOWN_AS]-(:Synonym) ";
		query += " WITH a, variant, p1, collect(synonyms) AS synonyms";
		query += " OPTIONAL MATCH notes=(variant:Variant)-[:ASSOCIATION]->(:Note) ";
		query += " WITH a, variant, p1, synonyms, collect(notes) AS notes";
		query += " OPTIONAL MATCH pubs=(variant:Variant)-[:ASSOCIATION]->(:Publication) ";
		query += " WITH a, variant, p1, synonyms, notes, collect(pubs) AS pubs";
		query += " OPTIONAL MATCH crossRefs=(variant:Variant)-[:CROSS_REFERENCE]->(:CrossReference) ";
		query += " WITH a, variant, p1, synonyms, notes, pubs, collect(crossRefs) AS crossRefs";
		query += " OPTIONAL MATCH consequence=(:GeneLevelConsequence)<-[:ASSOCIATION]-(variant:Variant)";
		query += " WITH a, variant, p1, synonyms, notes, pubs, crossRefs, collect(consequence) AS consequences";
		query += " OPTIONAL MATCH transcripts=(:GenomicLocation)--(variant:Variant)-[:ASSOCIATION]-(t:Transcript)<-[:TRANSCRIPT_TYPE]-(:SOTerm)";
		query += " WITH a, variant, p1, synonyms, notes, pubs, crossRefs, consequences, collect(transcripts) AS transcripts";
		query += " OPTIONAL MATCH transcriptConsequence=(variant:Variant)--(tlc:TranscriptLevelConsequence)<-[:ASSOCIATION]-(t:Transcript)--(variant:Variant)";
		query += " WITH a, variant, p1, synonyms, notes, pubs, crossRefs, consequences, transcripts, collect(transcriptConsequence) AS transcriptConsequences";
		query += " OPTIONAL MATCH loc=(variant:Variant)-[:ASSOCIATION]->(:GenomicLocation)";
		query += " WITH a, variant, p1, synonyms, notes, pubs, crossRefs, consequences, transcripts, transcriptConsequences, collect(loc) AS locs";
		query += " OPTIONAL MATCH p2=(variant:Variant)<-[:COMPUTED_GENE]-(:Gene)-[:ASSOCIATION]->(:GenomicLocation)";
		query += " WITH p1, collect(p2) AS p2, locs, consequences, synonyms, transcripts, transcriptConsequences, notes, crossRefs, pubs";
		query += " RETURN p1, p2, loc, consequences, synonyms, transcripts, transcriptConsequence, notes, crossRefs, pubs ";
		
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
		query += " WHERE variant.primaryKey = $" + paramName;
		query += " OPTIONAL MATCH consequence=(:GenomicLocation)--(variant:Variant)-[:ASSOCIATION]->(:TranscriptLevelConsequence)" +
				"<-[:ASSOCIATION]-(t:Transcript)<-[:TRANSCRIPT_TYPE]-(:SOTerm)";
		query += " OPTIONAL MATCH gene=(t:Transcript)-[:TRANSCRIPT]-(:Gene)--(:GenomicLocation)";
		query += " OPTIONAL MATCH transcriptLocation=(t:Transcript)-[:ASSOCIATION]-(:GenomicLocation)";
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

}
