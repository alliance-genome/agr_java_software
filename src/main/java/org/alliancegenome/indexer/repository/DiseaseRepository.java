package org.alliancegenome.indexer.repository;

import org.alliancegenome.indexer.entity.node.DOTerm;

import java.util.HashMap;
import java.util.List;

public class DiseaseRepository extends Neo4jRepository<DOTerm> {

    public DiseaseRepository() {
        super(DOTerm.class);
    }

    public List<DOTerm> getAllDiseaseTerms(int start, int maxSize) {
        String cypher = "match (root:DOTerm) WITH root SKIP " + start + " LIMIT " + maxSize + " " +
                "optional match (a:Association)-[q:ASSOCIATION]->(root), " +
                "(gene:Gene)-[qq:ASSOCIATION]->(a), " +
                "(publication:Publication)<-[publicationRelation]-(a), " +
                "(evidence:EvidenceCode)<-[evidenceRelation:EVIDENCE]-(a), " +
                "(species:Species)<-[speciesRelation:FROM_SPECIES]-(m), " +
                "(root)-[externalRelation:ALSO_KNOWN_AS]->(externalId:ExternalId) " +
                "WHERE root.is_obsolete <> 'true' " +
                "return distinct root, q,a,qq, gene,publicationRelation, publication, evidenceRelation, evidence, " +
                "species, speciesRelation, externalId, externalRelation ";
        return (List<DOTerm>) query(cypher);
    }

    public List<DOTerm> getAllDiseaseTerms() {
        String cypher = "match (root:DOTerm) " +
                "optional match (a:Association)-[q:ASSOCIATION]->(root), " +
                "(m:Gene)-[qq:ASSOCIATION]->(a), " +
                "(p:Publication)<-[qqq*]-(a), " +
                "(e:EvidenceCode)<-[ee:EVIDENCE]-(a), " +
                "(s:Species)<-[ss:FROM_SPECIES]-(m), " +
                "(root)-[ex:ALSO_KNOWN_AS]->(exx:ExternalId), " +
                "(root)-[synonymRelation:ALSO_KNOWN_AS]->(synonym:Synonym)  " +
                "optional match (parent:DOTerm)<-[parentRelation:IS_A]-(root:DOTerm), " +
                "(child:DOTerm)-[childRelation:IS_A]->(root:DOTerm) " +
                "return distinct root, q,a,qq,m,qqq,p, ee, e, s, ss, ex, exx, parent, " +
                "parentRelation, child, childRelation, synonymRelation, synonym ";
        return (List<DOTerm>) query(cypher);
    }

    public List<DOTerm> getDiseaseTermsWithAnnotations() {
        String cypher = "match (root:DOTerm),  " +
                "(a:Association)-[q:ASSOCIATION]->(root), " +
                "(gene:Gene)-[geneAssociation:ASSOCIATION]->(a), " +
                "(publication:Publication)<-[qqq]-(a), " +
                "(evidence:EvidenceCode)<-[evidenceRelation:EVIDENCE]-(a), " +
                "(species:Species)<-[speciesRelation:FROM_SPECIES]-(gene), " +
                "(root)-[parentChildRelation:IS_A*]->(parent:DOTerm) " +
                "return root, q,a, geneAssociation,gene ,qqq, publication, evidenceRelation, evidence, " +
                "species, speciesRelation, parentChildRelation, parent";

        return (List<DOTerm>) query(cypher);
    }

    public List<DOTerm> getAllTerms() {
        String cypher = "match (root:DOTerm) " +
                "optional match (parent:DOTerm)<-[parentRelation:IS_A]-(root:DOTerm), " +
                "(child:DOTerm)-[childRelation:IS_A]->(root:DOTerm), " +
                "(synonym:Synonym)-[synonymRelation:ALSO_KNOWN_AS]-(root:DOTerm) " +
                "return root, parent, " +
                "parentRelation, child, childRelation, synonym, synonymRelation";
        return (List<DOTerm>) query(cypher);
    }

    public long getDiseaseTermsWithAnnotationsCount() {
        String cypher = "match (root:DOTerm), " +
                "(a:Association)-[q:ASSOCIATION]->(root), " +
                "(m:Gene)-[qq:ASSOCIATION]->(a), " +
                "(p:Publication)<-[qqq*]-(a), " +
                "(e:EvidenceCode)<-[ee:EVIDENCE]-(a), " +
                "(s:Species)<-[ss:FROM_SPECIES]-(m), " +
                "(root)-[ex:ALSO_KNOWN_AS]->(exx:ExternalId) " +
                "return count(root)";
        Long s = queryCount(cypher);
        return s;
    }

    public DOTerm getDiseaseTerm(String primaryKey) {
        String cypher = "match (term:DOTerm), " +
                "(annotation:Association)-[annotationRelation]->(term), " +
                "(gene:Gene)-[geneAssociation:ASSOCIATION]->(annotation), " +
                "(publication:Publication)<-[publicationRelation:EVIDENCE]-(annotation), " +
                "(evidence:EvidenceCode)<-[evidenceRelation:EVIDENCE]-(annotation) " +
                "where term.primaryKey =  {primaryKey} " +
                "return term, annotation, annotationRelation, gene, geneAssociation, publication, publicationRelation, " +
                "evidence, evidenceRelation ";
        HashMap<String, String> map = new HashMap<>();
        map.put("primaryKey", primaryKey);

        DOTerm term = query(cypher, map).iterator().next();
        return term;
    }
}
