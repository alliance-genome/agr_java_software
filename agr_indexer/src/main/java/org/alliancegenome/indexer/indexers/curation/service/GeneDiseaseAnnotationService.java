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
import org.apache.commons.lang3.tuple.Pair;
import si.mazi.rescu.RestProxyFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Log4j2
public class GeneDiseaseAnnotationService extends BaseDiseaseAnnotationService {

	private final GeneDiseaseAnnotationInterface geneApi = RestProxyFactory.createProxy(GeneDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final GeneToGeneOrthologyGeneratedInterface orthologyApi = RestProxyFactory.createProxy(GeneToGeneOrthologyGeneratedInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private VocabularyService vocabService = new VocabularyService();
	private OrganizationService orgService = new OrganizationService();
	private ReferenceService referenceService = new ReferenceService();

	private final String cacheFileName = "gene_disease_annotation.json.gz";

	public List<GeneDiseaseAnnotation> getFiltered() {
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
		List<GeneDiseaseAnnotation> ret = new ArrayList<>();
		log.info("Gene IDs #: " + allGeneIDs);
		log.info("AGM IDs #: " + allModelIDs);

		int batchSize = 1000;
		int page = 0;
		int pages;

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		//params.put("subject.curie", "MGI:2140175");
		// params.put("subject.curie", "HGNC:6893");
//		params.put("subject.curie", "HGNC:40");

		do {
			SearchResponse<GeneDiseaseAnnotation> response = geneApi.findForPublic(page, batchSize, params);
			for (GeneDiseaseAnnotation da : response.getResults()) {
				if (isValidEntity(allGeneIDs, da.getSubjectCurie())) {
					if (hasValidGeneticModifiers(da, allGeneIDs, allAlleleIds, allModelIDs)) {
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

	public Map<Gene, List<DiseaseAnnotation>> getOrthologousGeneDiseaseAnnotations(Map<String, Pair<Gene, ArrayList<DiseaseAnnotation>>> geneMap) {
		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		params.put("strictFilter", true);

		VocabularyTerm isMarkerViaOrthology = vocabService.getDiseaseRelationTerms().get("is_marker_via_orthology");
		VocabularyTerm isImplicatedViaOrthology = vocabService.getDiseaseRelationTerms().get("is_implicated_via_orthology");
		// hard code MGI:6194238 with corresponding AGRKB ID
		Reference allianceReference = referenceService.getReference("AGRKB:101000000828456");

		Map<Gene, List<DiseaseAnnotation>> newDAMap = new HashMap<>();
		display.startProcess("Creating Gene DA's via orthology", geneMap.size());
		// loop over all Markers of validated DiseaseAnnotation records
		Set<String> geneIDs = geneMap.keySet();
/*
		geneIDs = new HashSet<>();
		geneIDs.add("MGI:2140175");
		geneIDs.add("HGNC:18640");
*/
		for (String geneID : geneIDs) {
			List<DiseaseAnnotation> focusDiseaseAnnotations = geneMap.get(geneID).getRight();
			params.put("subjectGene.curie", geneID);
			SearchResponse<GeneToGeneOrthologyGenerated> response = orthologyApi.find(0, 500, params);
			for (GeneToGeneOrthologyGenerated geneGeneOrthology : response.getResults()) {
				Gene orthologousGene = geneGeneOrthology.getObjectGene();
				if (!isValidEntity(allGeneIDs, orthologousGene.getCurie())) {
					continue;
				}
				// create orthologous DAs for each focus DA
				focusDiseaseAnnotations.forEach(focusDiseaseAnnotation -> {
					DiseaseAnnotation gda = null;
					if (focusDiseaseAnnotation instanceof AGMDiseaseAnnotation agmda) {
						AGMDiseaseAnnotation da = new AGMDiseaseAnnotation();
						da.setSubject(agmda.getSubject());
						gda = da;
					}
					if (focusDiseaseAnnotation instanceof AlleleDiseaseAnnotation ada) {
						AlleleDiseaseAnnotation da = new AlleleDiseaseAnnotation();
						da.setSubject(ada.getSubject());
						gda = da;
					}
					if (focusDiseaseAnnotation instanceof GeneDiseaseAnnotation gdann) {
						GeneDiseaseAnnotation da = new GeneDiseaseAnnotation();
						da.setSubject(gdann.getSubject());
						gda = da;
					}

					VocabularyTerm relation;
					if (focusDiseaseAnnotation.getRelation().getName().equals("is_marker_for")) {
						relation = isMarkerViaOrthology;
					} else {
						relation = isImplicatedViaOrthology;
					}
					gda.setRelation(relation);
					DataProvider dataProvider = new DataProvider();
					dataProvider.setSourceOrganization(orgService.getOrganization("Alliance"));
					gda.setDataProvider(dataProvider);
					gda.setWith(List.of(geneGeneOrthology.getSubjectGene()));
					gda.setSingleReference(allianceReference);
					gda.setObject(focusDiseaseAnnotation.getObject());
					gda.setEvidenceCodes(focusDiseaseAnnotation.getEvidenceCodes());
					gda.setDiseaseQualifiers(focusDiseaseAnnotation.getDiseaseQualifiers());
					List<DiseaseAnnotation> geneAnnotations = newDAMap.computeIfAbsent(orthologousGene, k -> new ArrayList<>());
					geneAnnotations.add(gda);
				});
			}
			display.progressProcess(1L);
		}
		display.finishProcess();
		// consolidating DAs
		newDAMap.forEach((gene, diseaseAnnotations) -> {
			Map<String, Map<String, List<DiseaseAnnotation>>> groupedDAs = diseaseAnnotations.stream().collect(groupingBy(da1 -> da1.getObject().getCurie(),
				groupingBy(da -> da.getRelation().getName())));
			groupedDAs.forEach((disease, relationListMap) -> {
				relationListMap.forEach((relation, daList) -> {
					Set<Gene> geneList = daList.stream().map(DiseaseAnnotation::getWith).flatMap(Collection::stream).collect(Collectors.toSet());
					daList.forEach(diseaseAnnotation -> diseaseAnnotation.setWith(new ArrayList<>(geneList)));
				});

			});
		});
		System.out.println("Number of orthologous genes generating new DAs: " + newDAMap.size());
		return newDAMap;
	}

}
