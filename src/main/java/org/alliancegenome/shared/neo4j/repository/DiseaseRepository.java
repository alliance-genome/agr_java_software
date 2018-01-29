package org.alliancegenome.shared.neo4j.repository;

import org.alliancegenome.shared.neo4j.entity.node.DOTerm;
import org.neo4j.ogm.exception.MappingException;
import org.neo4j.ogm.model.Result;

import static org.alliancegenome.shared.neo4j.entity.node.DOTerm.HIGH_LEVEL_TERM_LIST_SLIM;

import java.util.*;

public class DiseaseRepository extends Neo4jRepository<DOTerm> {

	public DiseaseRepository() {
		super(DOTerm.class);
	}

//	public List<DOTerm> getAllDiseaseTerms(int start, int maxSize) {
//		String cypher = "match (root:DOTerm) WHERE	root.is_obsolete = 'false' " +
//				"WITH root SKIP " + start + " LIMIT " + maxSize + " " +
//				"optional match (diseaseGeneJoin:DiseaseGeneJoin)-[q:ASSOCIATION]->(root), " +
//				"(gene:Gene)-[geneDiseaseRelation:ASSOCIATION]->(diseaseGeneJoin), " +
//				"(publication:Publication)<-[publicationRelation]-(diseaseGeneJoin), " +
//				"(evidence:EvidenceCode)<-[evidenceRelation:EVIDENCE]-(diseaseGeneJoin), " +
//				"(species:Species)<-[speciesRelation:FROM_SPECIES]-(gene), " +
//				"(root)-[crossReferenceRelation:CROSS_REFERENCE]->(crossReference:CrossReference) " +
//				"return distinct root, q,diseaseGeneJoin, geneDiseaseRelation, gene, publicationRelation, publication, evidenceRelation, evidence, " +
//				"species, speciesRelation, crossReference, crossReferenceRelation ";
//		return (List<DOTerm>) query(cypher);
//	}

//	public List<DOTerm> getAllDiseaseTerms() {
//		String cypher = "match (root:DOTerm) " +
//				"optional match (a:Association)-[q:ASSOCIATION]->(root), " +
//				"(m:Gene)-[qq:ASSOCIATION]->(a), " +
//				"(p:Publication)<-[qqq*]-(a), " +
//				"(e:EvidenceCode)<-[ee:EVIDENCE]-(a), " +
//				"(s:Species)<-[ss:FROM_SPECIES]-(m), " +
//				"(root)-[ex:CROSS_REFERENCE]->(exx:CrossReference), " +
//				"(root)-[synonymRelation:ALSO_KNOWN_AS]->(synonym:Synonym)	" +
//				"optional match (parent:DOTerm)<-[parentRelation:IS_A]-(root:DOTerm), " +
//				"(child:DOTerm)-[childRelation:IS_A]->(root:DOTerm) " +
//				"return distinct root, q,a,qq,m,qqq,p, ee, e, s, ss, ex, exx, parent, " +
//				"parentRelation, child, childRelation, synonymRelation, synonym ";
//		return (List<DOTerm>) query(cypher);
//	}


//	public List<DOTerm> getDiseaseTermsWithAnnotations() {
//		String cypher = "match (root:DOTerm),  " +
//				"(a:Association)-[q:ASSOCIATION]->(root), " +
//				"(gene:Gene)-[geneAssociation:ASSOCIATION]->(a), " +
//				"(publication:Publication)<-[qqq]-(a), " +
//				"(evidence:EvidenceCode)<-[evidenceRelation:EVIDENCE]-(a), " +
//
//				"(root)-[parentChildRelation:IS_A*]->(parent:DOTerm) " +
//				"return root, q,a, geneAssociation,gene ,qqq, publication, evidenceRelation, evidence, " +
//				"species, speciesRelation, parentChildRelation, parent";
//
//		return (List<DOTerm>) query(cypher);

	//DiseaseGeneJoin
	public DOTerm getDiseaseTermWithAnnotations(String primaryKey) {
		String cypher = "match p0=(root:DOTerm)--(a:Association)-[:EVIDENCE]-(pe),"
				+ " p1=(a)-[:ASSOCIATION]-(gene:Gene)-[:FROM_SPECIES]-(species:Species),"
				+ " p2=(root)-[:IS_A*]->(p:DOTerm) WHERE root.primaryKey = {primaryKey}" +
				" RETURN p0, p1, p2";

		HashMap<String, String> map = new HashMap<>();
		map.put("primaryKey", primaryKey);

		try {
			Iterable<DOTerm> terms = query(cypher, map);
			for (DOTerm d : terms) {
				if (d.getPrimaryKey().equals(primaryKey)) {
					return d;
				}
			}
		} catch (MappingException e) {
			e.printStackTrace();
		}
		return null;

	}

//	public List<DOTerm> getAllTerms() {
//		String cypher = "match (root:DOTerm) " +
//				"optional match (parent:DOTerm)<-[parentRelation:IS_A]-(root:DOTerm), " +
//				"(child:DOTerm)-[childRelation:IS_A]->(root:DOTerm), " +
//				"(synonym:Synonym)<-[synonymRelation:ALSO_KNOWN_AS]-(root:DOTerm) " +
//				"return root, parent, " +
//				"parentRelation, child, childRelation, synonym, synonymRelation";
//		return (List<DOTerm>) query(cypher);
//	}
//
//	public long getDiseaseTermsWithAnnotationsCount() {
//		String cypher = "match (root:DOTerm), " +
//				"(a:Association)-[q:ASSOCIATION]->(root), " +
//				"(m:Gene)-[qq:ASSOCIATION]->(a), " +
//				"(p:Publication)<-[qqq*]-(a), " +
//				"(e:EvidenceCode)<-[ee:EVIDENCE]-(a), " +
//				"(s:Species)<-[ss:FROM_SPECIES]-(m), " +
//				"(root)-[ex:CROSS_REFERENCE]->(exx:CrossReference) " +
//				"return count(root)";
//		Long s = queryCount(cypher);
//		return s;
//	}

	public List<String> getAllDiseaseKeys() {
		String query = "MATCH (term:DOTerm) WHERE term.is_obsolete='false' RETURN term.primaryKey";
		
		Result r = queryForResult(query);
		Iterator<Map<String, Object>> i = r.iterator();

		ArrayList<String> list = new ArrayList<>();

		while (i.hasNext()) {
			Map<String, Object> map2 = i.next();
			list.add((String) map2.get("term.primaryKey"));
		}
		return list;
	}

	public DOTerm getDiseaseTerm(String primaryKey) {

		String cypher = "MATCH p0=(d:DOTerm)--(s) WHERE d.primaryKey = {primaryKey}	 " +
				" OPTIONAL MATCH p1=(d)--(s:DiseaseGeneJoin)-[:EVIDENCE]-(eq), p2=(s)--(g:Gene)-[:FROM_SPECIES]-(species:Species)" +
				" OPTIONAL MATCH p3=(d)-[:IS_A]-(d2)" +
				" OPTIONAL MATCH slim=(d)-[:IS_A*]->(slTerm) " +
				" where all (subset IN [{subset}] where subset in slTerm.subset) " +
				" RETURN p0, p1, p2, p3, slim";

		HashMap<String, String> map = new HashMap<>();
		map.put("primaryKey", primaryKey);
		map.put("subset", HIGH_LEVEL_TERM_LIST_SLIM);

		DOTerm primaryTerm = null;
		List<DOTerm> highLevelTermList = new ArrayList<>(3);
		try {
			Iterable<DOTerm> terms = query(cypher, map);
			for (DOTerm term : terms) {
				if (term.getPrimaryKey().equals(primaryKey)) {
					primaryTerm = term;
				}
				if (term.getSubset().contains(HIGH_LEVEL_TERM_LIST_SLIM))
					highLevelTermList.add(term);
			}
		} catch (MappingException e) {
			e.printStackTrace();
		}
		if(primaryTerm == null)
			return null;
		primaryTerm.getHighLevelTermList().addAll(highLevelTermList);
		return primaryTerm;
	}
}
