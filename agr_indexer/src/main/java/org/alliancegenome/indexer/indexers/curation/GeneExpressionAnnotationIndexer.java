package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import org.alliancegenome.curation_api.interfaces.document.GeneExpressionDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.curation_api.model.entities.CrossReference;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.apache.commons.collections.CollectionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class GeneExpressionAnnotationIndexer extends Indexer {

	private final GeneExpressionDocumentInterface geneExpressionApi = RestProxyFactory.createProxy(GeneExpressionDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	
	private HashMap<String, Object> params = new HashMap<>() {{
		put("internal", false);
		put("obsolete", false);
	}};
	
	public GeneExpressionAnnotationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index() {
		try {
			SearchResponse<GeneExpressionDocument> response = geneExpressionApi.findDocument(0, 0, params);
			log.info("GeneExpressionAnnotation count: " + response.getTotalResults());
			int totalPages = (int) (response.getTotalResults() / indexerConfig.getBufferSize());
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i <= totalPages; i++) {
				queue.add(String.valueOf(i));
			}
			
			initiateThreading(queue);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		HashMap<String, List<GeneExpressionDocument>> geneExpressionMap = new HashMap<>();
		List<GeneExpressionDocument> documentsToIndex = new ArrayList<>();
		String currentGeneId = null;
		while (true) {
			try {
				if (queue.isEmpty()) {
					if (currentGeneId != null) {
						List<GeneExpressionDocument> docs = geneExpressionMap.get(currentGeneId);
						if (docs != null && !docs.isEmpty()) {
							documentsToIndex.addAll(docs);
						}
					}
					consolidateAndIndexDocuments(documentsToIndex);
					return;
				}

				String page = queue.takeFirst();
				log.debug(queue.size() + " pages to process " + Thread.currentThread().getName() + " starting page: " + page);
				SearchResponse<GeneExpressionDocument> response = geneExpressionApi.findDocument(Integer.valueOf(page), indexerConfig.getBufferSize(), params);
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					if (currentGeneId != null) {
						List<GeneExpressionDocument> docs = geneExpressionMap.get(currentGeneId);
						if (docs != null && !docs.isEmpty()) {
							documentsToIndex.addAll(docs);
						}
					}
					consolidateAndIndexDocuments(documentsToIndex);
					return;
				}
				for (GeneExpressionDocument ged : response.getResults()) {
					Gene gene = ged.getGeneExpressionAnnotation().getExpressionAnnotationSubject();
					if (gene != null) {
						String incomingGeneId = ged.getGeneExpressionAnnotation().getExpressionAnnotationSubject().getPrimaryExternalId();
						HashMap<String, Integer> order = SpeciesType.getSpeciesOrderByTaxonID(gene.getTaxon().getCurie());
						ged.setSpeciesOrder(order);
						if (currentGeneId != null && !currentGeneId.equals(incomingGeneId)) {
							documentsToIndex.addAll(geneExpressionMap.get(currentGeneId));
							geneExpressionMap.remove(currentGeneId);
						}

						currentGeneId = incomingGeneId;
						geneExpressionMap.compute(currentGeneId, (key, existingList) -> {
							if (existingList == null) {
								existingList = new ArrayList<>();
							}
							existingList.add(ged);
							return existingList;
						});
					}
				}
				consolidateAndIndexDocuments(documentsToIndex);

			} catch (Exception e) {
				log.error("Error while indexing...", e);
				System.exit(-1);
				return;
			}
		}
	}

	protected void consolidateAndIndexDocuments(List<GeneExpressionDocument> documents) {
		try {
			if (!documents.isEmpty()) {
				List<GeneExpressionDocument> consolidatedDocuments = consolidateExpressionDocuments(documents);
				int batches = (int) Math.ceil((double) consolidatedDocuments.size() / indexerConfig.getBufferSize());
				for (int i = 1; i <= batches; i++) {
					int start = (i - 1) * indexerConfig.getBufferSize();
					int end = Math.min(i * indexerConfig.getBufferSize(), consolidatedDocuments.size());
					indexDocuments(consolidatedDocuments.subList(start, end));
				}
				documents.clear();
			}
		} catch (Exception e) {
			log.error("Error indexing GeneExpressionAnnotations", e);
		}
	}

	private List<GeneExpressionDocument> consolidateExpressionDocuments(List<GeneExpressionDocument> documents) {
		Map<String, List<GeneExpressionDocument>> groupedDocuments = documents.stream()
			.collect(Collectors.groupingBy(doc -> {
				String geneId = doc.getGeneExpressionAnnotation().getExpressionAnnotationSubject() != null
					? doc.getGeneExpressionAnnotation().getExpressionAnnotationSubject().getPrimaryExternalId() : "";
				String location = doc.getGeneExpressionAnnotation().getWhereExpressedStatement() != null
					? doc.getGeneExpressionAnnotation().getWhereExpressedStatement() : "";
				String stage = doc.getGeneExpressionAnnotation().getWhenExpressedStageName() != null
					? doc.getGeneExpressionAnnotation().getWhenExpressedStageName() : "";
				String assay = doc.getGeneExpressionAnnotation().getExpressionAssayUsed() != null
					? doc.getGeneExpressionAnnotation().getExpressionAssayUsed().getCurie() : "";

				return geneId + "||" + location + "||" + stage + "||" + assay;
			}));

		List<GeneExpressionDocument> consolidatedDocuments = new ArrayList<>();

		for (Map.Entry<String, List<GeneExpressionDocument>> entry : groupedDocuments.entrySet()) {
			List<GeneExpressionDocument> group = entry.getValue();

			if (group.size() == 1) {
				consolidatedDocuments.add(group.get(0));
			} else {
				GeneExpressionDocument consolidated = group.get(0);

				List<CrossReference> allCrossReferences = new ArrayList<>();
				List<String> allReferenceIds = new ArrayList<>();

				// logic behind this consolidation is to have 1:1 mapping for reference and crossReference for consolidated annotations, which is useful while deconsolidating later for download endpoint
				for (GeneExpressionDocument doc : group) {
					int annotationSize = Math.max(
						CollectionUtils.isNotEmpty(doc.getGeneExpressionAnnotation().getCrossReferences()) ? doc.getGeneExpressionAnnotation().getCrossReferences().size() : 0,
						CollectionUtils.isNotEmpty(doc.getReferenceId()) ? doc.getReferenceId().size() : 0
					);
					for (int i = 0; i < annotationSize; i++) {
						if (CollectionUtils.isNotEmpty(doc.getGeneExpressionAnnotation().getCrossReferences()) && i < doc.getGeneExpressionAnnotation().getCrossReferences().size()) {
							allCrossReferences.add(doc.getGeneExpressionAnnotation().getCrossReferences().get(i));
						} else {
							CrossReference emptyRef = new CrossReference();
							emptyRef.setDisplayName("");
							emptyRef.setReferencedCurie("");
							allCrossReferences.add(emptyRef);
						}
						if (CollectionUtils.isNotEmpty(doc.getReferenceId())) {
							// Incoming geneexpression annotation always has only one referenceId
							allReferenceIds.add(doc.getReferenceId().get(0));
						}
					}
				}

				consolidated.getGeneExpressionAnnotation().setCrossReferences(allCrossReferences);
				consolidated.setReferenceId(allReferenceIds);

				consolidatedDocuments.add(consolidated);
			}
		}

		return consolidatedDocuments;
	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	}
}


