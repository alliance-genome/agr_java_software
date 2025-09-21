package org.alliancegenome.indexer.indexers.curation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.model.entities.*;
import org.alliancegenome.curation_api.model.entities.associations.AlleleConstructAssociation;
import org.alliancegenome.curation_api.model.entities.associations.AlleleGeneAssociation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.interfaces.AlleleConstructAssociationInterface;
import org.alliancegenome.indexer.indexers.curation.interfaces.AlleleGeneAssociationInterface;
import org.alliancegenome.indexer.indexers.curation.interfaces.AlleleInterface;
import org.alliancegenome.indexer.indexers.curation.interfaces.ResourceDescriptorPageInterface;
import org.alliancegenome.indexer.indexers.curation.service.BaseService;
import org.apache.commons.collections.CollectionUtils;
import si.mazi.rescu.RestProxyFactory;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

@Slf4j
public class AlleleSummaryCurationIndexer extends Indexer {

	private final AlleleConstructAssociationInterface alleleConstructAssociationApi = RestProxyFactory
		.createProxy(AlleleConstructAssociationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final AlleleInterface alleleApi = RestProxyFactory.createProxy(AlleleInterface.class,
		ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final AlleleGeneAssociationInterface alleleGeneAssociationApi = RestProxyFactory
		.createProxy(AlleleGeneAssociationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final ResourceDescriptorPageInterface resourceDescriptorPageApi = RestProxyFactory
		.createProxy(ResourceDescriptorPageInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final BaseService baseService = new BaseService();
	private Set<String> allNeoAlleleIDs = baseService.getAllNeoAlleleIDs();
	private HashMap<String, Object> params = new HashMap<>() {
		{
			put("internal", false);
			put("obsolete", false);
		}
	};

	public AlleleSummaryCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index() {
		Map<String, ResourceDescriptorPage> resourceDescriptorPageMap = buildResourceDescriptorPageMap();
		Map<Allele, Gene> alleleOfGeneMap = buildAlleleOfGeneMap();
		Map<Allele, List<AlleleConstructAssociation>> alleleConstructMap = buildAlleleConstructAssociationsMap();
		indexAlleles(alleleConstructMap, alleleOfGeneMap, resourceDescriptorPageMap);
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
	}

	private Map<Allele, List<AlleleConstructAssociation>> buildAlleleConstructAssociationsMap() {
		SearchResponse<AlleleConstructAssociation> alleleConstructCountResponse = alleleConstructAssociationApi
			.findForPublic(0, 0, params);
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
		display.startProcess("Pulling allele construct associations from curation",
			alleleConstructCountResponse.getTotalResults());
		Map<Allele, List<AlleleConstructAssociation>> documentMap = new LinkedHashMap<>();
		int batchSize = 1000;
		int maxPage = (int) (alleleConstructCountResponse.getTotalResults() / batchSize);
		for (int page = 0; page <= maxPage; page++) {
			SearchResponse<AlleleConstructAssociation> response = alleleConstructAssociationApi.findForPublic(page,
				batchSize, params);
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

	private Map<Allele, Gene> buildAlleleOfGeneMap() {
		HashMap<String, Object> agaParams = new HashMap<>() {
			{
				put("internal", false);
				put("obsolete", false);
				put("relation.name", "is_allele_of");
			}
		};

		SearchResponse<AlleleGeneAssociation> alleleGeneCountResponse = alleleGeneAssociationApi.findForPublic(0, 0,
			agaParams);
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
		display.startProcess("Pulling allele gene associations from curation", alleleGeneCountResponse.getTotalResults());
		Map<Allele, Gene> documentMap = new LinkedHashMap<>();
		int batchSize = 1000;
		int maxPage = (int) (alleleGeneCountResponse.getTotalResults() / batchSize);
		for (int page = 0; page <= maxPage; page++) {
			SearchResponse<AlleleGeneAssociation> response = alleleGeneAssociationApi.findForPublic(page, batchSize,
				agaParams);
			for (AlleleGeneAssociation alleleGeneAssociation : response.getResults()) {
				if (alleleGeneAssociation == null) {
					continue;
				}
				Allele allele = alleleGeneAssociation.getAlleleAssociationSubject();
				Gene gene = alleleGeneAssociation.getAlleleGeneAssociationObject();

				documentMap.put(allele, gene);
			}
			display.progressProcess(response.getReturnedRecords().longValue());
		}
		return documentMap;
	}

	private Map<String, ResourceDescriptorPage> buildResourceDescriptorPageMap() {
		HashMap<String, Object> rdpParams = new HashMap<>() {
			{
				put("internal", false);
				put("obsolete", false);
				put("name", "allele/references");
			}
		};
		SearchResponse<ResourceDescriptorPage> resourceDescriptorPageCountResponse = resourceDescriptorPageApi
			.findForPublic(0, 0, rdpParams);
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
		display.startProcess("Pulling resource descriptor pages from curation",
			resourceDescriptorPageCountResponse.getTotalResults());
		Map<String, ResourceDescriptorPage> documentMap = new LinkedHashMap<>();
		int batchSize = 1000;
		int maxPage = (int) (resourceDescriptorPageCountResponse.getTotalResults() / batchSize);
		for (int page = 0; page <= maxPage; page++) {
			SearchResponse<ResourceDescriptorPage> response = resourceDescriptorPageApi.findForPublic(page, batchSize,
				rdpParams);
			for (ResourceDescriptorPage resourceDescriptorPage : response.getResults()) {
				if (resourceDescriptorPage == null) {
					continue;
				}

				documentMap.put(resourceDescriptorPage.getResourceDescriptor().getPrefix(), resourceDescriptorPage);
			}
			display.progressProcess(response.getReturnedRecords().longValue());
		}
		return documentMap;
	}

	private void indexAlleles(Map<Allele, List<AlleleConstructAssociation>> alleleConstructMap,
							Map<Allele, Gene> alleleOfGeneMap,
							Map<String, ResourceDescriptorPage> resourceDescriptorPageMap) {
		SearchResponse<Allele> alleleCountResponse = alleleApi.findForPublic(0, 0, params);
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
		display.startProcess("Pulling Allele documents from curation", alleleCountResponse.getTotalResults());
		Map<Allele, AlleleSummaryDocument> documentMap = new LinkedHashMap<>();
		int batchSize = 10;
		int maxPage = (int) (alleleCountResponse.getTotalResults() / batchSize);
		for (int page = 0; page <= maxPage; page++) {
			SearchResponse<Allele> response = alleleApi.findForPublic(page, batchSize, params);
			for (Allele allele : response.getResults()) {
				if (allele == null) {
					continue;
				}
				AlleleSummaryDocument alleleSummaryDocument = new AlleleSummaryDocument();
				alleleSummaryDocument.setAllele(allele);
				alleleSummaryDocument.setConstructSlimList(getConstructs(alleleConstructMap.get(allele)));
				alleleSummaryDocument.setAlleleOfGene(alleleOfGeneMap.get(allele));
				alleleSummaryDocument.setCrossReference(getCrossReference(allele, resourceDescriptorPageMap));
				alleleSummaryDocument.setAlterationType(determineAlterationType(allele));
				alleleSummaryDocument.setDescription(buildDescription(allele));

				documentMap.put(allele, alleleSummaryDocument);

				display.progressProcess(response.getReturnedRecords().longValue());
			}
		}

		Collection<AlleleSummaryDocument> alleleSummaryDocuments = documentMap.values();
		List<AlleleSummaryDocument> filteredDocs = filterAgainstNeo(alleleSummaryDocuments);
		indexDocuments(filteredDocs);
	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	}

	private List<AlleleSummaryDocument> filterAgainstNeo(Collection<AlleleSummaryDocument> docs) {
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

	private CrossReference getCrossReference(Allele allele,
											 Map<String, ResourceDescriptorPage> resourceDescriptorPageMap) {

		CrossReference alleleRefsCrossRef = new CrossReference();

		if (allele.getDataProvider() == null) {
			return alleleRefsCrossRef;
		}

		String dataProviderAbbreviation = allele.getDataProvider().getAbbreviation();
		ResourceDescriptorPage page = resourceDescriptorPageMap.get(dataProviderAbbreviation);

		if (page != null && allele.getDataProviderCrossReference() != null) {
			alleleRefsCrossRef.setReferencedCurie(allele.getDataProviderCrossReference().getReferencedCurie());
			alleleRefsCrossRef.setDisplayName(allele.getDataProviderCrossReference().getDisplayName());
			alleleRefsCrossRef.setResourceDescriptorPage(page);
		}

		return alleleRefsCrossRef;
	}

	private String determineAlterationType(Allele allele) {
		if (allele.getAlleleVariantAssociations() == null || allele.getAlleleVariantAssociations().isEmpty()) {
			return "allele";
		} else if (allele.getAlleleVariantAssociations().size() == 1) {
			return "allele with one variant";
		} else {
			return "allele with multiple variants";
		}
	}

	private String buildDescription(Allele allele) {

		String description = "";

		if (CollectionUtils.isNotEmpty(allele.getRelatedNotes())) {
			List<String> descriptionList = allele.getRelatedNotes()
				.stream()
				.filter(note -> note.getNoteType().getName().equals("mutation_description"))
				.map(note -> note.getFreeText())
				.collect(Collectors.toList());

			if (CollectionUtils.isNotEmpty(descriptionList)) {
				description = descriptionList.get(0);
			}
		}

		return description;

	}

}
