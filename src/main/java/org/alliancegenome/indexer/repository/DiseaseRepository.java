package org.alliancegenome.indexer.repository;

import java.util.List;

import org.alliancegenome.indexer.entity.node.DOTerm;

public class DiseaseRepository extends Neo4jRepository<DOTerm> {

    public DiseaseRepository() {
        super(DOTerm.class);
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
                "return root, q,a,qq,m,qqq,p, ee, e, s, ss, ex, exx, parent, " +
                "parentRelation, child, childRelation, synonymRelation, synonym ";
        return (List<DOTerm>) query(cypher);
    }

    public List<DOTerm> getDiseaseTermsWithAnnotations() {
        String cypher = "match (root:DOTerm), " +
                "(a:Association)-[q:ASSOCIATION]->(root), " +
                "(m:Gene)-[qq:ASSOCIATION]->(a), " +
                "(p:Publication)<-[qqq*]-(a), " +
                "(e:EvidenceCode)<-[ee:EVIDENCE]-(a), " +
                "(s:Species)<-[ss:FROM_SPECIES]-(m), " +
                "(root)-[ex:ALSO_KNOWN_AS]->(exx:ExternalId), " +
                "(root)-[synonymRelation:ALSO_KNOWN_AS]->(synonym:Synonym)  " +
                "optional match (parent:DOTerm)<-[parentRelation:IS_A]-(root:DOTerm), " +
                "(child:DOTerm)-[childRelation:IS_A]->(root:DOTerm) " +
                "return root, q,a,qq,m,qqq,p, ee, e, s, ss, ex, exx, parent, " +
                "parentRelation, child, childRelation, synonymRelation, synonym ";
        return (List<DOTerm>) query(cypher);
    }
}
