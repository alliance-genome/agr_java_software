package org.alliancegenome.indexer.indexers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.disease.DiseaseDocument;
import org.alliancegenome.indexer.entity.DOTerm;
import org.alliancegenome.indexer.service.Neo4jService;
import org.alliancegenome.indexer.translators.DiseaseToESDiseaseTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiseaseIndexer extends Indexer<DiseaseDocument> {


	private Logger log = LogManager.getLogger(getClass());

	private Neo4jService<DOTerm> neo4jService = new Neo4jService<>(DOTerm.class);
	private DiseaseToESDiseaseTranslator diseaseToSI = new DiseaseToESDiseaseTranslator();

	public DiseaseIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	public void index() {


		Neo4jService<DOTerm> neo4jService = new Neo4jService<>(DOTerm.class);
		String cypher = "match (n:DOTerm), " +
				"(a:Annotation)-[q:ASSOCIATION]->(n), " +
				"(m:Gene)-[qq:ASSOCIATION]->(a), " +
				"(p:Publication)<-[qqq*]-(a), " +
				"(e:EvidenceCode)<-[ee:ANNOTATED_TO]-(p)" +
				"return n, q,a,qq,m,qqq,p, ee, e";
		List<DOTerm> geneDiseaseList = (List<DOTerm>)neo4jService.query(cypher);

		cypher = "match (n:DOTerm)<-[q:IS_A]-(m:DOTerm)<-[r:IS_IMPLICATED_IN]-(g:Gene) return n,q, m";
		List<DOTerm> geneDiseaseInfoList = (List<DOTerm>)neo4jService.query(cypher);
		Map<String, DOTerm> infoMap = geneDiseaseInfoList.stream()
				.collect((Collectors.toMap(DOTerm::getPrimaryKey, id -> id)));
		List<DOTerm> geneDiseaseCompleteList = geneDiseaseList.stream()
				.peek(doTerm -> {
					if (infoMap.get(doTerm.getPrimaryKey()) != null)
						doTerm.setParents(infoMap.get(doTerm.getPrimaryKey()).getParents());
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
