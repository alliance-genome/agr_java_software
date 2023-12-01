package org.alliancegenome.indexer.indexers.curation.service;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.*;
import org.alliancegenome.curation_api.model.entities.orthology.GeneToGeneOrthologyGenerated;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneDiseaseAnnotationInterface;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneToGeneOrthologyGeneratedInterface;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import si.mazi.rescu.RestProxyFactory;

import java.util.*;

@Log4j2
public class GeneDiseaseAnnotationService extends BaseDiseaseAnnotationService {

	private final GeneDiseaseAnnotationInterface geneApi = RestProxyFactory.createProxy(GeneDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final GeneToGeneOrthologyGeneratedInterface orthologyApi = RestProxyFactory.createProxy(GeneToGeneOrthologyGeneratedInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private VocabularyService vocabService = new VocabularyService();
	private OrganizationService orgService = new OrganizationService();
	private ReferenceService referenceService = new ReferenceService();

	public List<GeneDiseaseAnnotation> getFiltered() {
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
		List<GeneDiseaseAnnotation> ret = new ArrayList<>();
		GeneRepository geneRepository = new GeneRepository();
		AlleleRepository alleleRepository = new AlleleRepository();
		HashSet<String> alleleIds = new HashSet<>(alleleRepository.getAllAlleleIDs());
		HashSet<String> allGeneIDs = new HashSet<>(geneRepository.getAllGeneKeys());
		HashSet<String> allModelIDs = new HashSet<>(alleleRepository.getAllModelKeys());
		log.info("Allele IDs #: "+alleleIds);
		log.info("Gene IDs #: "+allGeneIDs);
		log.info("AGM IDs #: "+allModelIDs);

		int batchSize = 1000;
		int page = 0;
		int pages;

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		//params.put("subject.curie", "WB:WBGene00013606");
		//params.put("subject.curie", "RGD:2004");

		do {
			SearchResponse<GeneDiseaseAnnotation> response = geneApi.findForPublic(page, batchSize, params);
			for(GeneDiseaseAnnotation da: response.getResults()) {
				if(isValidEntity(allGeneIDs, da.getSubjectCurie())) {
					if (hasValidGeneticModifiers(da, allGeneIDs, alleleIds, allModelIDs)) {
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
		geneRepository.close();
		alleleRepository.close();

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

		int batchSize = 360;
		// loop over all Markers of validated GeneDiseaseAnnotation records
		for (String geneID : geneMap.keySet()) {
			List<DiseaseAnnotation> focusDiseaseAnnotations = geneMap.get(geneID).getRight();
			params.put("subjectGene.curie", geneID);
			int page = 0;
			int pages;
			do {
				SearchResponse<GeneToGeneOrthologyGenerated> response = orthologyApi.find(page, batchSize, params);
				for (GeneToGeneOrthologyGenerated geneGeneOrthology : response.getResults()) {
					Gene orthologousGene = geneGeneOrthology.getObjectGene();
					if (!isValidEntity(allGeneIDs, orthologousGene.getCurie())) {
						continue;
					}

					// create orthologous DAs for each focus DA that is a Gene DA
					focusDiseaseAnnotations.stream().filter(diseaseAnnotation -> diseaseAnnotation instanceof GeneDiseaseAnnotation)
						.forEach(focusDiseaseAnnotation -> {
							GeneDiseaseAnnotation gda = new GeneDiseaseAnnotation();

							VocabularyTerm relation = null;
							if (focusDiseaseAnnotation.getRelation().getName().equals("is_marker_for")) {
								relation = vocabService.getVocabularyTerm("is_marker_via_orthology");
							} else if (focusDiseaseAnnotation.getRelation().getName().equals("is_implicated_in")) {
								relation = vocabService.getVocabularyTerm("is_implicated_via_orthology");
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
							ret.add(gda);
						});
				}

				if (page == 0) {
					display.startProcess("Creating Gene DA's via orthology", response.getTotalResults());
				}
				display.progressProcess(response.getReturnedRecords().longValue());
				pages = (int) (response.getTotalResults() / batchSize);
				page++;
			} while (page <= pages);
		}
		display.finishProcess();
		geneRepository.close();

		return ret;
	}

	private static boolean hasValidGenes(GeneDiseaseAnnotation da, HashSet<String> allGeneIDs) {
		if (da.getInternal())
			return false;
		if (!allGeneIDs.contains(da.getSubject().getCurie()))
			return false;
		if (CollectionUtils.isNotEmpty(da.getWith())) {

			if (da.getWith().stream().anyMatch((gene -> !allGeneIDs.contains(gene.getCurie()))))
				return false;
		}
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifiers())) {
			return da.getDiseaseGeneticModifiers().stream()
				.noneMatch((gene -> !allGeneIDs.contains(gene.getCurie())));
		}
		return true;
	}

}
