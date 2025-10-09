package org.alliancegenome.indexer.indexers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.api.entity.GeneTransgenicAlleleSummaryDocument;
import org.alliancegenome.api.entity.TransgenicAlleleSummaryDocument;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.TransgenicAlleleDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.TransgenicAlleleDTO;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.Construct;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.TransgenicAlleleConstruct;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import si.mazi.rescu.RestProxyFactory;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class TransgenicAlleleIndexer extends Indexer {

	private final TransgenicAlleleDocumentInterface transgenicAlleleApi = RestProxyFactory.createProxy(TransgenicAlleleDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private final HashMap<String, Object> params = new HashMap<>() {{
		put("internal", false);
		put("obsolete", false);
		//put("alleleAssociationSubject.primaryExternalId", "WB:WBGene00000936");
	}};

	public TransgenicAlleleIndexer(IndexerConfig config) {
		super(config);

	}

	@Override
	protected void index() {
		List<TransgenicAlleleSummaryDocument> alleleList = indexTransgenicAlleleSummary();
		indexTransgenicAlleleAnnotations(alleleList);
	}

	private void indexTransgenicAlleleAnnotations(List<TransgenicAlleleSummaryDocument> documents) {
		Map<Gene, List<GeneTransgenicAlleleSummaryDocument>> geneMap = new LinkedHashMap<>();
		documents.forEach(transgenicAlleleSummaryDocument -> {
			// obtain affected genes per document
			transgenicAlleleSummaryDocument.getTransgenicAlleleConstructs().forEach(transgenicAlleleConstruct -> {
				if (CollectionUtils.isNotEmpty(transgenicAlleleConstruct.getExpressedGenes())) {
					transgenicAlleleConstruct.getExpressedGenes().forEach(gene -> {
						List<GeneTransgenicAlleleSummaryDocument> geneList = geneMap.computeIfAbsent(gene, k -> new ArrayList<>());
						GeneTransgenicAlleleSummaryDocument document = new GeneTransgenicAlleleSummaryDocument(gene);
						document.setAlleleDocument(transgenicAlleleSummaryDocument);
						geneList.add(document);
					});
				}
				if (CollectionUtils.isNotEmpty(transgenicAlleleConstruct.getNonBgiComponents())) {
					transgenicAlleleConstruct.getNonBgiComponents().forEach(gene -> {
						List<GeneTransgenicAlleleSummaryDocument> geneList = geneMap.computeIfAbsent(gene, k -> new ArrayList<>());
						GeneTransgenicAlleleSummaryDocument document = new GeneTransgenicAlleleSummaryDocument(gene);
						document.setAlleleDocument(transgenicAlleleSummaryDocument);
						geneList.add(document);
					});
				}
				if (CollectionUtils.isNotEmpty(transgenicAlleleConstruct.getRegulatoryGenes())) {
					transgenicAlleleConstruct.getRegulatoryGenes().forEach(gene -> {
						List<GeneTransgenicAlleleSummaryDocument> geneList = geneMap.computeIfAbsent(gene, k -> new ArrayList<>());
						GeneTransgenicAlleleSummaryDocument document = new GeneTransgenicAlleleSummaryDocument(gene);
						document.setAlleleDocument(transgenicAlleleSummaryDocument);
						geneList.add(document);
					});
				}
			});
		});
		List<GeneTransgenicAlleleSummaryDocument> lists = geneMap.values().stream().flatMap(Collection::stream).toList();
		indexDocuments(lists);
	}

	private List<TransgenicAlleleSummaryDocument> indexTransgenicAlleleSummary() {
		SearchResponse<TransgenicAlleleDTO> searchResponse = transgenicAlleleApi.findDocuments(0, 0, params);
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
		display.startProcess("Pulling Transgenic Alleles from curation", searchResponse.getTotalResults());
		Map<Allele, TransgenicAlleleSummaryDocument> documentMap = new LinkedHashMap<>();
		int batchSize = indexerConfig.getBufferSize();
		int maxPage = (int) (searchResponse.getTotalResults() / batchSize);
		for (int page = 0; page <= maxPage; page++) {
			SearchResponse<TransgenicAlleleDTO> response = transgenicAlleleApi.findDocuments(page, batchSize, params);
			for (TransgenicAlleleDTO da : response.getResults()) {
				if (da == null) {
					continue;
				}
				TransgenicAlleleSummaryDocument document = documentMap.computeIfAbsent(da.getAllele(), allele -> {
					TransgenicAlleleSummaryDocument doc = new TransgenicAlleleSummaryDocument();
					doc.setAllele(allele);
					return doc;
				});
				List<TransgenicAlleleConstruct> constructList = document.getTransgenicAlleleConstructs();
				if (constructList == null) {
					constructList = new ArrayList<>();
					document.setTransgenicAlleleConstructs(constructList);
				}
				TransgenicAlleleConstruct construct = new TransgenicAlleleConstruct();
				construct.setConstruct(da.getConstruct());
				construct.setExpressedGenes(getExpressedGenes(da.getConstruct()));
				construct.setRegulatoryGenes(getRegulatoryGenes(da.getConstruct()));
				document.setHasDiseaseAnnotations(da.getHasDiseaseAnnotations());
				document.setHasPhenotypeAnnotations(da.getHasPhenotypeAnnotations());
				constructList.add(construct);
			}
			display.progressProcess(response.getReturnedRecords().longValue());
		}
		display.finishProcess();
		Collection<TransgenicAlleleSummaryDocument> values = documentMap.values();
		indexDocuments(new ArrayList<>(new HashSet<>(values)));
		return new ArrayList<>(values);
	}

	private List<Gene> getRegulatoryGenes(Construct construct) {
		return getGenes(construct, "is_regulated_by");
	}

	@NotNull
	private static List<Gene> getGenes(Construct construct, String relationName) {
		List<Gene> expressedGenes = new ArrayList<>();
		construct.getConstructGenomicEntityAssociations().forEach(constructGenomicEntityAssociation -> {
			if (constructGenomicEntityAssociation.getConstructGenomicEntityAssociationObject() instanceof Gene gene) {
				if (constructGenomicEntityAssociation.getRelation().getName().equals(relationName)) {
					expressedGenes.add(gene);
				}
			}
		});
		return expressedGenes;
	}

	private List<Gene> getExpressedGenes(Construct construct) {
		return getGenes(construct, "expresses");
	}


	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	}
}
