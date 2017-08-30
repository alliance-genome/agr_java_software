package org.alliancegenome.indexer.indexers;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.DiseaseDocument;
import org.alliancegenome.indexer.entity.DOTerm;
import org.alliancegenome.indexer.repository.DiseaseRepository;
import org.alliancegenome.indexer.translators.DiseaseTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiseaseIndexer extends Indexer<DiseaseDocument> {

	private Logger log = LogManager.getLogger(getClass());

	private DiseaseRepository repo = new DiseaseRepository();
	private DiseaseTranslator diseaseTrans = new DiseaseTranslator();

	public DiseaseIndexer(String currnetIndex, TypeConfig config) {
		super(currnetIndex, config);
	}



	@Override
	public void index() {

		String cypher = "MATCH (parent:DOTerm)<-[parentRelation:IS_A]-(root:DOTerm)<-[r:IS_IMPLICATED_IN]-(Gene)," +
				"(root)-[ss:ALSO_KNOWN_AS]->(synonym:Synonym)  " +
				"OPTIONAL MATCH (root)<-[childRelation:IS_A]-(child:DOTerm) " +
				"return root, child, childRelation, parent, parentRelation, synonym";
		List<DOTerm> geneDiseaseInfoList = (List<DOTerm>)repo.query(cypher);
		Map<String, DOTerm> infoMap = geneDiseaseInfoList.stream()
				.collect((Collectors.toMap(DOTerm::getPrimaryKey, id -> id)));

		cypher = "match (n:DOTerm), " +
				"(a:Association)-[q:ASSOCIATION]->(n), " +
				"(m:Gene)-[qq:ASSOCIATION]->(a), " +
				"(p:Publication)<-[qqq*]-(a), " +
				"(e:EvidenceCode)<-[ee:EVIDENCE]-(a), " +
				"(s:Species)<-[ss:FROM_SPECIES]-(m), " +
				"(n)-[ex:ALSO_KNOWN_AS]->(exx:ExternalId)" +
				"return n, q,a,qq,m,qqq,p, ee, e, s, ss, ex, exx";
		List<DOTerm> geneDiseaseList = (List<DOTerm>)repo.query(cypher);	

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
				addDocuments(diseaseTrans.translateEntities(geneDiseaseCompleteList));
				progress(i, pages, chunkSize);
			}
			finishProcess(diseaseCount);
		}

	}

}
