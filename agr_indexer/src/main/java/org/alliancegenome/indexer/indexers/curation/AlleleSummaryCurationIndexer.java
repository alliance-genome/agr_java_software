package org.alliancegenome.indexer.indexers.curation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.api.entity.TransgenicAlleleSummaryDocument;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.Construct;
import org.alliancegenome.curation_api.model.entities.associations.AlleleConstructAssociation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.interfaces.AlleleConstructAssociationInterface;
import org.alliancegenome.indexer.indexers.curation.interfaces.AlleleInterface;
import org.alliancegenome.indexer.indexers.curation.service.BaseService;
import org.apache.commons.collections.CollectionUtils;
import si.mazi.rescu.RestProxyFactory;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class AlleleSummaryCurationIndexer extends Indexer {

	private final AlleleConstructAssociationInterface alleleConstructAssociationApi = RestProxyFactory.createProxy(AlleleConstructAssociationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final AlleleInterface alleleApi = RestProxyFactory.createProxy(AlleleInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final BaseService baseService = new BaseService();
	private Set<String> allNeoAlleleIDs = baseService.getAllNeoAlleleIDs();
	private HashMap<String, Object> params = new HashMap<>() {{
		put("internal", false);
		put("obsolete", false);
	}};

	public AlleleSummaryCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index() {
		Map<Allele, List<AlleleConstructAssociation>> alleleConstructMap = indexAlleleConstructAssociations();
		indexAlleles(alleleConstructMap);
	}


	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
	}

	private Map<Allele, List<AlleleConstructAssociation>> indexAlleleConstructAssociations() {
		SearchResponse<AlleleConstructAssociation> alleleSummaryResponse = alleleConstructAssociationApi.findForPublic(0, 0, params);
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
		display.startProcess("Pulling Allele documents from curation", alleleSummaryResponse.getTotalResults());
		Map<Allele, List<AlleleConstructAssociation>> documentMap = new LinkedHashMap<>();
		int batchSize = 1000;
		int maxPage = (int) (alleleSummaryResponse.getTotalResults() / batchSize);
		for (int page = 0; page <= maxPage; page++) {
			SearchResponse<AlleleConstructAssociation> response = alleleConstructAssociationApi.findForPublic(page, batchSize, params);
			for (AlleleConstructAssociation alleleConstructAssociation : response.getResults()) {
				if (alleleConstructAssociation == null) {
					continue;
				}
				Allele allele = alleleConstructAssociation.getAlleleAssociationSubject();
				List<AlleleConstructAssociation> list = documentMap.computeIfAbsent(allele, k -> new ArrayList<>());
				list.add(alleleConstructAssociation);
			}
			display.progressProcess(response.getReturnedRecords().longValue());
		}
		return documentMap;
	}

	private List<TransgenicAlleleSummaryDocument> indexAlleles(Map<Allele, List<AlleleConstructAssociation>> alleleConstructMap) {
		SearchResponse<Allele> alleleSummaryResponse = alleleApi.findForPublic(0, 0, params);
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
		display.startProcess("Pulling Allele documents from curation", alleleSummaryResponse.getTotalResults());
		Map<Allele, TransgenicAlleleSummaryDocument> documentMap = new LinkedHashMap<>();
		int batchSize = 10;
		int maxPage = (int) (alleleSummaryResponse.getTotalResults() / batchSize);
		for (int page = 0; page <= maxPage; page++) {
			SearchResponse<Allele> response = alleleApi.findForPublic(page, batchSize, params);
			for (Allele dallele : response.getResults()) {
				if (dallele == null) {
					continue;
				}
				AlleleSummaryDocument alleleSummaryDocument = new AlleleSummaryDocument();
				alleleSummaryDocument.setAllele(dallele);
				alleleSummaryDocument.setConstructSlimList(getConstructs(alleleConstructMap.get(dallele)));

				display.progressProcess(response.getReturnedRecords().longValue());
			}
			Collection<TransgenicAlleleSummaryDocument> values = documentMap.values();
			indexDocuments(new ArrayList<>(new HashSet<>(values)));
			return new ArrayList<>(values);
		}
		return null;
	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	}

	private List<AlleleSummaryDocument> filterAgainstNeo(List<AlleleSummaryDocument> docs) {
		List<AlleleSummaryDocument> result = new ArrayList<>();
		for (AlleleSummaryDocument doc : docs) {
			String identifier = doc.getAllele().getPrimaryExternalId();
			if (allNeoAlleleIDs.contains(identifier)) {
				result.add(doc);
			}
		}
		return result;
	}

	private List<Construct> getConstructs(List<AlleleConstructAssociation> associations) {
		List<Construct> constructs = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(associations)) {
			for (AlleleConstructAssociation association : associations) {
				Construct construct = association.getAlleleConstructAssociationObject();
				if (construct != null) {
					constructs.add(construct);
				}
			}
		}
		return constructs;
	}

}
