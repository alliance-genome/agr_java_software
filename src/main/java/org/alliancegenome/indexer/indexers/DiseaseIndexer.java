package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.disease.DiseaseDocument;
import org.alliancegenome.indexer.entity.DOTerm;
import org.alliancegenome.indexer.service.Neo4jService;
import org.alliancegenome.indexer.translators.DiseaseToESDiseaseTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DiseaseIndexer extends Indexer<DiseaseDocument> {


    private Logger log = LogManager.getLogger(getClass());

    private Neo4jService<DOTerm> neo4jService = new Neo4jService<>(DOTerm.class);
    private DiseaseToESDiseaseTranslator diseaseToSI = new DiseaseToESDiseaseTranslator();

    public DiseaseIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    public void index() {

        String cypher = "match (n:DOTerm), " +
                "(a:DiseaseGeneJoin)-[q:ASSOCIATION]->(n), " +
                "(m:Gene)-[qq:ASSOCIATION]->(a), " +
                "(p:Publication)<-[qqq*]-(a), " +
                "(e:EvidenceCode)<-[ee:EVIDENCE]-(a), " +
                "(s:Species)<-[ss:FROM_SPECIES]-(m), " +
                "(n)-[ex:ALSO_KNOWN_AS]->(exx:ExternalId)" +
                "return n, q,a,qq,m,qqq,p, ee, e, s, ss, ex, exx";
        List<DOTerm> geneDiseaseList = (List<DOTerm>) neo4jService.query(cypher);

        cypher = "MATCH (parent:DOTerm)<-[parentRelation:IS_A]-(root:DOTerm)<-[r:IS_IMPLICATED_IN]-(Gene)," +
                "(root)-[ss:ALSO_KNOWN_AS]->(synonym:Synonym)  " +
                "OPTIONAL MATCH (root)<-[childRelation:IS_A]-(child:DOTerm) " +
                "return root, child, childRelation, parent, parentRelation, synonym";
        List<DOTerm> geneDiseaseInfoList = (List<DOTerm>) neo4jService.query(cypher);
        Map<String, DOTerm> infoMap = geneDiseaseInfoList.stream()
                .collect((Collectors.toMap(DOTerm::getPrimaryKey, id -> id)));
        List<DOTerm> geneDiseaseCompleteList = geneDiseaseList.stream()
                .map(doTerm -> {
                    if (infoMap.get(doTerm.getPrimaryKey()) != null)
                        doTerm.setParents(infoMap.get(doTerm.getPrimaryKey()).getParents());
                    return doTerm;
                })
                .map(doTerm -> {
                    if (infoMap.get(doTerm.getPrimaryKey()) != null)
                        doTerm.setChildren(infoMap.get(doTerm.getPrimaryKey()).getChildren());
                    return doTerm;
                })
                .collect(Collectors.toList());

        int diseaseCount = geneDiseaseCompleteList.size();
        int chunkSize = 1000;
        int pages = diseaseCount / chunkSize;

        log.debug("DiseaseCount: " + diseaseCount);


        if (diseaseCount > 0) {
            startProcess(pages, chunkSize, diseaseCount);
            for (int i = 0; i <= pages; i++) {
                addDocuments(diseaseToSI.translateEntities(geneDiseaseCompleteList));
                progress(i, pages, chunkSize);
            }
            finishProcess(diseaseCount);
        }

    }

}
