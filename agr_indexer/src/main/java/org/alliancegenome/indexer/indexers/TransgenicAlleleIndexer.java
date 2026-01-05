package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.GeneTransgenicAlleleSummaryDocument;
import org.alliancegenome.api.entity.TransgenicAlleleSummaryDocument;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.TransgenicAlleleDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.TransgenicAlleleDTO;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.Construct;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.TransgenicAlleleConstruct;
import org.alliancegenome.curation_api.model.entities.slotAnnotations.ConstructComponentSlotAnnotation;
import org.alliancegenome.curation_api.model.entities.slotAnnotations.GeneSymbolSlotAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class TransgenicAlleleIndexer extends Indexer {

	private final TransgenicAlleleDocumentInterface transgenicAlleleApi = RestProxyFactory.createProxy(TransgenicAlleleDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private final HashMap<String, Object> params = new HashMap<>() {{
		put("internal", false);
		put("obsolete", false);
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
		Map<Gene, Set<GeneTransgenicAlleleSummaryDocument>> geneMap = new LinkedHashMap<>();
		documents.forEach(transgenicAlleleSummaryDocument -> {
			// obtain affected genes per document
			transgenicAlleleSummaryDocument.getTransgenicAlleleConstructs().forEach(transgenicAlleleConstruct -> {
				if (CollectionUtils.isNotEmpty(transgenicAlleleConstruct.getExpressedGenes())) {
					addGenesToMap(transgenicAlleleConstruct.getExpressedGenes(), geneMap, transgenicAlleleSummaryDocument);
				}
				if (CollectionUtils.isNotEmpty(transgenicAlleleConstruct.getRegulatoryGenes())) {
					addGenesToMap(transgenicAlleleConstruct.getRegulatoryGenes(), geneMap, transgenicAlleleSummaryDocument);
				}
				if (CollectionUtils.isNotEmpty(transgenicAlleleConstruct.getTargetedGenes())) {
					addGenesToMap(transgenicAlleleConstruct.getTargetedGenes(), geneMap, transgenicAlleleSummaryDocument);
				}
			});
		});
		List<GeneTransgenicAlleleSummaryDocument> lists = geneMap.values().stream().flatMap(Collection::stream).toList();
		indexDocuments(lists);
	}

	private void addGenesToMap(List<Gene> genes, Map<Gene, Set<GeneTransgenicAlleleSummaryDocument>> geneMap, TransgenicAlleleSummaryDocument transgenicAlleleSummaryDocument) {
		genes.forEach(gene -> {
			Set<GeneTransgenicAlleleSummaryDocument> geneList = geneMap.computeIfAbsent(gene, k -> new HashSet<>());
			GeneTransgenicAlleleSummaryDocument document = new GeneTransgenicAlleleSummaryDocument(gene);
			document.setAlleleDocument(transgenicAlleleSummaryDocument);
			geneList.add(document);
		});

	}

	private List<TransgenicAlleleSummaryDocument> indexTransgenicAlleleSummary() {
		SearchResponse<TransgenicAlleleDTO> searchResponse = transgenicAlleleApi.findDocuments(0, 0, params);
		ProcessDisplayHelper display = new ProcessDisplayHelper();
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
					doc.setPhylogeneticSortingIndex(allele.getTaxon().getPhylogeneticSortOrder());
					return doc;
				});
				List<TransgenicAlleleConstruct> constructList = document.getTransgenicAlleleConstructs();
				if (constructList == null) {
					constructList = new ArrayList<>();
					document.setTransgenicAlleleConstructs(constructList);
				}
				Construct constructObj = da.getConstruct();

				TransgenicAlleleConstruct construct = new TransgenicAlleleConstruct();
				// MGI special handling
				String primaryExternalId = constructObj.getPrimaryExternalId();
				if (constructObj.getPlaceholder() != null && constructObj.getPlaceholder() && ObjectUtils.isEmpty(primaryExternalId)) {
					String modInternalId = constructObj.getModInternalId();
					if (modInternalId != null) {
						constructObj.setPrimaryExternalId(modInternalId);
					}
				}
				construct.setConstruct(constructObj);
				construct.setExpressedGenes(getExpressedGenes(constructObj));
				construct.setRegulatoryGenes(getRegulatoryGenes(constructObj));
				construct.setTargetedGenes(getTargetedGenes(constructObj));
				document.setHasDiseaseAnnotations(da.getHasDiseaseAnnotations());
				document.setHasPhenotypeAnnotations(da.getHasPhenotypeAnnotations());
				document.setPhylogeneticSortingIndex(da.getAllele().getTaxon().getPhylogeneticSortOrder());
				constructList.add(construct);
			}
			display.progressProcess(response.getReturnedRecords().longValue());
		}
		display.finishProcess();
		Collection<TransgenicAlleleSummaryDocument> values = documentMap.values();
		indexDocuments(new ArrayList<>(new HashSet<>(values)));
		return new ArrayList<>(values);
	}

	private Gene getNonBgiComponent(ConstructComponentSlotAnnotation annotation) {
		Gene nonBgiGene = new Gene();
		GeneSymbolSlotAnnotation symbol = new GeneSymbolSlotAnnotation();
		symbol.setDisplayText(annotation.getComponentSymbol());
		symbol.setFormatText(annotation.getComponentSymbol());
		nonBgiGene.setGeneSymbol(symbol);
		return nonBgiGene;
	}

	private List<Gene> getRegulatoryGenes(Construct construct) {
		return getGenes(construct, "is_regulated_by");
	}

	private List<Gene> getTargetedGenes(Construct construct) {
		return getGenes(construct, "targets");
	}

	private List<Gene> getGenes(Construct construct, String relationName) {
		List<Gene> expressedGenes = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(construct.getConstructGenomicEntityAssociations())) {
			construct.getConstructGenomicEntityAssociations().forEach(constructGenomicEntityAssociation -> {
				if (constructGenomicEntityAssociation.getConstructGenomicEntityAssociationObject() instanceof Gene gene) {
					if (constructGenomicEntityAssociation.getRelation().getName().equals(relationName)) {
						expressedGenes.add(gene);
					}
				}
			});
		}
		if (CollectionUtils.isNotEmpty(construct.getConstructComponents())) {
			construct.getConstructComponents().forEach(constructComponent -> {
				if (constructComponent.getRelation().getName().equals(relationName)) {
					expressedGenes.add(getNonBgiComponent(constructComponent));
				}
			});
		}
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
