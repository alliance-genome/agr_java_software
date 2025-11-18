package org.alliancegenome.indexer.indexers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.api.entity.VariantSummaryDocument;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.VariantDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDTO;
import org.alliancegenome.curation_api.model.entities.*;
import org.alliancegenome.curation_api.model.entities.slotAnnotations.ConstructComponentSlotAnnotation;
import org.alliancegenome.curation_api.model.entities.slotAnnotations.GeneSymbolSlotAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.apache.commons.collections4.CollectionUtils;
import si.mazi.rescu.RestProxyFactory;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class VariantSummaryIndexer extends Indexer {

	private final VariantDocumentInterface transgenicAlleleApi = RestProxyFactory.createProxy(VariantDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private final HashMap<String, Object> params = new HashMap<>() {{
		put("internal", false);
		put("obsolete", false);
	}};

	public VariantSummaryIndexer(IndexerConfig config) {
		super(config);

	}

	@Override
	protected void index() {
		List<Variant> variantList = indexVariantSummaryDocuments();
	}


	private List<Variant> indexVariantSummaryDocuments() {
		SearchResponse<VariantSummaryDTO> searchResponse = transgenicAlleleApi.findDocuments(0, 0, params);
		ProcessDisplayHelper display = new ProcessDisplayHelper();
		display.startProcess("Pulling Transgenic Alleles from curation", searchResponse.getTotalResults());
		Map<Allele, VariantSummaryDTO> documentMap = new LinkedHashMap<>();
		List<VariantSummaryDocument> list = new ArrayList<>();
		int batchSize = indexerConfig.getBufferSize();
		int maxPage = (int) (searchResponse.getTotalResults() / batchSize);
		for (int page = 0; page <= maxPage; page++) {
			SearchResponse<VariantSummaryDTO> response = transgenicAlleleApi.findDocuments(page, batchSize, params);
			for (VariantSummaryDTO da : response.getResults()) {
				if (da == null) {
					continue;
				}
				VariantSummaryDocument document = new VariantSummaryDocument();
				document.setVariant(da.getVariant());
				list.add(document);
			}
			display.progressProcess(response.getReturnedRecords().longValue());
		}
		display.finishProcess();
		indexDocuments(list);
		return new ArrayList<>();
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
