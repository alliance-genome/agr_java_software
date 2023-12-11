package org.alliancegenome.indexer.indexers.curation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.DataProvider;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Reference;
import org.alliancegenome.curation_api.model.entities.VocabularyTerm;
import org.alliancegenome.curation_api.model.entities.orthology.GeneToGeneOrthologyGenerated;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneDiseaseAnnotationInterface;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneToGeneOrthologyGeneratedInterface;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.lang3.tuple.Pair;

import lombok.extern.log4j.Log4j2;
import si.mazi.rescu.RestProxyFactory;

@Log4j2
public class GeneDiseaseAnnotationService extends BaseDiseaseAnnotationService {

	private final GeneDiseaseAnnotationInterface geneApi = RestProxyFactory.createProxy(GeneDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final GeneToGeneOrthologyGeneratedInterface orthologyApi = RestProxyFactory.createProxy(GeneToGeneOrthologyGeneratedInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private VocabularyService vocabService = new VocabularyService();
	private OrganizationService orgService = new OrganizationService();
	private ReferenceService referenceService = new ReferenceService();

	private String cacheFileName = "gene_disease_annotation.json.gz";

	public List<GeneDiseaseAnnotation> getFiltered() {
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
		List<GeneDiseaseAnnotation> ret = new ArrayList<>();
		GeneRepository geneRepository = new GeneRepository();
		log.info("Gene IDs #: " + allGeneIDs);
		log.info("AGM IDs #: " + allModelIDs);

		int batchSize = 1000;
		int page = 0;
		int pages;

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		// params.put("subject.curie", "WB:WBGene00013606");
		// params.put("subject.curie", "HGNC:6893");
//		params.put("subject.curie", "HGNC:40");

		do {
			SearchResponse<GeneDiseaseAnnotation> response = geneApi.findForPublic(page, batchSize, params);
			for(GeneDiseaseAnnotation da: response.getResults()) {
				if(isValidEntity(allGeneIDs, da.getSubjectCurie())) {
					if (hasValidGeneticModifiers(da, allGeneIDs, allAlleleIds, allModelIDs)) {
						// TODO: add json view in curation endpoint to not retrieve construct info which
						// makes the object huge. construct info is not needed for disease annotations
						da.getSubject().getConstructGenomicEntityAssociations().clear();
						ret.add(da);
					}
				}
			}

			if (page == 0) {
				display.startProcess("Pulling Gene DA's from curation", response.getTotalResults());
			}
			display.progressProcess(response.getReturnedRecords().longValue());
			pages = (int) (response.getTotalResults() / batchSize);
			page++;
		} while (page <= pages);
		display.finishProcess();

		writeToCache(cacheFileName, ret);

		return ret;
	}

	public List<GeneDiseaseAnnotation> getOrthologousGeneDiseaseAnnotations(Map<String, Pair<Gene, ArrayList<DiseaseAnnotation>>> geneMap) {
		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);
		List<GeneDiseaseAnnotation> ret = new ArrayList<>();
		GeneRepository geneRepository = new GeneRepository();
		HashSet<String> allGeneIDs = new HashSet<>(geneRepository.getAllGeneKeys());

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		params.put("strictFilter", true);

		long count = 0;
		display.startProcess("Creating Gene DA's via orthology", geneMap.size());
		// loop over all Markers of validated GeneDiseaseAnnotation records
		for (String geneID : geneMap.keySet()) {
			List<DiseaseAnnotation> focusDiseaseAnnotations = geneMap.get(geneID).getRight();
			params.put("subjectGene.curie", geneID);
			SearchResponse<GeneToGeneOrthologyGenerated> response = orthologyApi.find(0, 500, params);
			for (GeneToGeneOrthologyGenerated geneGeneOrthology : response.getResults()) {
				Gene orthologousGene = geneGeneOrthology.getObjectGene();
				if (!isValidEntity(allGeneIDs, orthologousGene.getCurie())) {
					continue;
				}

				// create orthologous DAs for each focus DA that is a Gene DA
				focusDiseaseAnnotations.stream().filter(diseaseAnnotation -> diseaseAnnotation instanceof GeneDiseaseAnnotation).forEach(focusDiseaseAnnotation -> {
					GeneDiseaseAnnotation gda = new GeneDiseaseAnnotation();

					VocabularyTerm relation = null;
					if (focusDiseaseAnnotation.getRelation().getName().equals("is_marker_for")) {
						relation = vocabService.getDiseaseRelationTerms().get("is_marker_via_orthology");
					} else if (focusDiseaseAnnotation.getRelation().getName().equals("is_implicated_in")) {
						relation = vocabService.getDiseaseRelationTerms().get("is_implicated_via_orthology");
					}
					if (relation == null) {
						throw new RuntimeException("No valid association type found for gene DA for given geneID: " + geneID);
					}
					gda.setRelation(relation);
					DataProvider dataProvider = new DataProvider();
					dataProvider.setSourceOrganization(orgService.getOrganization("Alliance"));
					gda.setDataProvider(dataProvider);
					gda.setWith(List.of(geneGeneOrthology.getSubjectGene()));
					gda.setSubject(orthologousGene);
					// hard code MGI:6194238 with corresponding AGRKB ID
					Reference reference = referenceService.getReference("AGRKB:101000000828456");
					gda.setSingleReference(reference);
					gda.setObject(focusDiseaseAnnotation.getObject());
					gda.setEvidenceCodes(focusDiseaseAnnotation.getEvidenceCodes());
					gda.setDiseaseQualifiers(focusDiseaseAnnotation.getDiseaseQualifiers());
					ret.add(gda);
				});
			}
			display.progressProcess(1L);
		}
		display.finishProcess();
		geneRepository.close();
		System.out.println("Number of DAs with via orthology: " + ret.size());
		return ret;
	}

}
