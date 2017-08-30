package org.alliancegenome.indexer.repository;

import org.alliancegenome.indexer.entity.DOTerm;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiseaseRepository extends Neo4jRepository<DOTerm> {

    public DiseaseRepository() {
        super(DOTerm.class);
    }

    public List<DOTerm> getAllDiseaseTermsWithAnnotations() {
        String cypher = "MATCH (parent:DOTerm)<-[parentRelation:IS_A]-(root:DOTerm)<-[r:IS_IMPLICATED_IN]-(Gene)," +
                "(root)-[synonymRelation:ALSO_KNOWN_AS]->(synonym:Synonym)  " +
                "OPTIONAL MATCH (root)<-[childRelation:IS_A]-(child:DOTerm) " +
                "return root, child, childRelation, parent, parentRelation, synonym, synonymRelation";
        List<DOTerm> doTermList = (List<DOTerm>) query(cypher);

        Map<String, DOTerm> infoMap = doTermList.stream()
                .collect((Collectors.toMap(DOTerm::getPrimaryKey, id -> id)));

        List<DOTerm> diseaseWithParentChildren = getDoTermsWithChildrenAndParents();

        List<DOTerm> geneDiseaseCompleteList = diseaseWithParentChildren.stream()
                .peek(doTerm -> {
                    if (infoMap.get(doTerm.getPrimaryKey()) != null)
                        doTerm.setParents(infoMap.get(doTerm.getPrimaryKey()).getParents());
                })
                .peek(doTerm -> {
                    if (infoMap.get(doTerm.getPrimaryKey()) != null)
                        doTerm.setChildren(infoMap.get(doTerm.getPrimaryKey()).getChildren());
                })
                .collect(Collectors.toList());

        return geneDiseaseCompleteList;
    }

    public List<DOTerm> getDoTermsWithChildrenAndParents() {
        String cypher = "match (n:DOTerm), " +
                "(a:Association)-[q:ASSOCIATION]->(n), " +
                "(m:Gene)-[qq:ASSOCIATION]->(a), " +
                "(p:Publication)<-[qqq*]-(a), " +
                "(e:EvidenceCode)<-[ee:EVIDENCE]-(a), " +
                "(s:Species)<-[ss:FROM_SPECIES]-(m), " +
                "(n)-[ex:ALSO_KNOWN_AS]->(exx:ExternalId)" +
                "return n, q,a,qq,m,qqq,p, ee, e, s, ss, ex, exx";
        return (List<DOTerm>) query(cypher);
    }
}
