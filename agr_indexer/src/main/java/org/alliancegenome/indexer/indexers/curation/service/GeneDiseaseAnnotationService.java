package org.alliancegenome.indexer.indexers.curation.service;

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Organization;
import org.alliancegenome.curation_api.model.entities.Reference;
import org.alliancegenome.curation_api.model.entities.VocabularyTerm;
import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;
import org.alliancegenome.curation_api.model.entities.orthology.GeneToGeneOrthologyGenerated;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneDiseaseAnnotationInterface;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneToGeneOrthologyGeneratedInterface;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import lombok.extern.log4j.Log4j2;
import si.mazi.rescu.RestProxyFactory;

@Log4j2
public class GeneDiseaseAnnotationService extends BaseDiseaseAnnotationService {

	private final GeneDiseaseAnnotationInterface geneApi = RestProxyFactory.createProxy(GeneDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final GeneToGeneOrthologyGeneratedInterface orthologyApi = RestProxyFactory.createProxy(GeneToGeneOrthologyGeneratedInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private VocabularyTermService vocabularyTermService = new VocabularyTermService();
	private EcoTermService ecoTermService = new EcoTermService();
	private OrganizationService orgService = new OrganizationService();
	private ReferenceService referenceService = new ReferenceService();

	private final String cacheFileName = "gene_disease_annotation.json.gz";

	public List<GeneDiseaseAnnotation> getFiltered() {
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);

		List<GeneDiseaseAnnotation> ret = readFromCache(cacheFileName, List.class);
		if (ret != null && ret.size() > 0) {
			return ret;
		} else {
			ret = new ArrayList<>();
		}

		int batchSize = 1000;
		int page = 0;
		int pages;

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		//params.put("diseaseAnnotationSubject.primaryExternalId", "RGD:69258");

		do {
			SearchResponse<GeneDiseaseAnnotation> response = geneApi.findForPublic(page, batchSize, params);
			for (GeneDiseaseAnnotation da : response.getResults()) {
				if (isValidNeoEntity(getAllNeoGeneIDs(), da.getDiseaseAnnotationSubject().getIdentifier()) && hasNoObsoletedOrInternalEntities(da)) {
					if (hasValidGeneticModifiers(da, getAllNeoGeneIDs(), getAllNeoAlleleIDs(), getAllNeoModelIDs())) {
						ret.add(da);
					}
				}
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

		String orthoCacheFileName = "gene_disease_via_orthology_annotation.json.gz";
		
		HashMap<Gene, List<DiseaseAnnotation>> newDAMap = readFromCache(orthoCacheFileName, HashMap.class);
		
		if (newDAMap == null) {
			newDAMap = new HashMap<>();
		}
		
		if (newDAMap != null && newDAMap.size() > 0) {
			return newDAMap;
		}
		
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		params.put("strictFilter", true);

		VocabularyTerm isMarkerViaOrthology = vocabularyTermService.getDiseaseRelationTerms().get("is_marker_via_orthology");
		VocabularyTerm isImplicatedViaOrthology = vocabularyTermService.getDiseaseRelationTerms().get("is_implicated_via_orthology");
		ECOTerm ecoTermIEA = ecoTermService.getEcoTerm("ECO:0000501");
		// hard code MGI:6194238 with corresponding AGRKB ID
		Reference allianceReference = referenceService.getReference("AGRKB:101000000828456");

		
		display.startProcess("Creating Gene DA's via orthology", geneMap.size());
		// loop over all Markers of validated DiseaseAnnotation records
		Set<String> geneIDs = geneMap.keySet();
/*
		geneIDs = new HashSet<>();
		geneIDs.add("HGNC:2865");
		geneIDs.add("MGI:94891");
*/
		for (String geneID : geneIDs) {
			List<DiseaseAnnotation> focusDiseaseAnnotations = geneMap.get(geneID).getRight();
			params.put("subjectGene.primaryExternalId", geneID);
			SearchResponse<GeneToGeneOrthologyGenerated> response = orthologyApi.findForPublic(0, 500, params);
			for (GeneToGeneOrthologyGenerated geneGeneOrthology : response.getResults()) {
				Gene orthologousGene = geneGeneOrthology.getObjectGene();
				if (!isValidNeoEntity(getAllNeoGeneIDs(), orthologousGene.getIdentifier()) || orthologousGene.getObsolete() || orthologousGene.getInternal()) {
					continue;
				}
				// create orthologous DAs for each focus DA
				
				for (DiseaseAnnotation focusDiseaseAnnotation: focusDiseaseAnnotations) {

					DiseaseAnnotation gda = null;
					if (focusDiseaseAnnotation instanceof AGMDiseaseAnnotation agmda) {
						AGMDiseaseAnnotation da = new AGMDiseaseAnnotation();
						da.setDiseaseAnnotationSubject(agmda.getDiseaseAnnotationSubject());
						gda = da;
					}
					if (focusDiseaseAnnotation instanceof AlleleDiseaseAnnotation ada) {
						AlleleDiseaseAnnotation da = new AlleleDiseaseAnnotation();
						da.setDiseaseAnnotationSubject(ada.getDiseaseAnnotationSubject());
						gda = da;
					}
					if (focusDiseaseAnnotation instanceof GeneDiseaseAnnotation gdann) {
						GeneDiseaseAnnotation da = new GeneDiseaseAnnotation();
						da.setDiseaseAnnotationSubject(gdann.getDiseaseAnnotationSubject());
						gda = da;
					}

					VocabularyTerm relation;
					if (focusDiseaseAnnotation.getRelation().getName().equals("is_marker_for")) {
						relation = isMarkerViaOrthology;
					} else {
						relation = isImplicatedViaOrthology;
					}
					gda.setRelation(relation);
					Organization dataProvider = orgService.getOrganization("Alliance");
					gda.setDataProvider(dataProvider);
					gda.setWith(List.of(geneGeneOrthology.getSubjectGene()));
					gda.setSingleReference(allianceReference);
					gda.setDiseaseAnnotationObject(focusDiseaseAnnotation.getDiseaseAnnotationObject());
					gda.setEvidenceCodes(List.of(ecoTermIEA));
					gda.setDiseaseQualifiers(focusDiseaseAnnotation.getDiseaseQualifiers());
					
					List<DiseaseAnnotation> geneAnnotations = newDAMap.computeIfAbsent(orthologousGene, k -> new ArrayList<>());
					geneAnnotations.add(gda);
				}
			}
			display.progressProcess();
		}
		display.finishProcess();
		// consolidating DAs:
		// by: disease, relation and disease qualifier
		newDAMap.forEach((gene, diseaseAnnotations) -> {
			Map<String, Map<String, Map<String, List<DiseaseAnnotation>>>> groupedDAs = diseaseAnnotations.stream().collect(groupingBy(da1 -> da1.getDiseaseAnnotationObject().getCurie(),
				groupingBy(da -> da.getRelation().getName(), groupingBy(da -> {
					List<VocabularyTerm> diseaseQualifiers = da.getDiseaseQualifiers();
					// allow for grouping by missing based-on genes
					if (CollectionUtils.isEmpty(diseaseQualifiers)) {
						return "null";
					}
					return diseaseQualifiers.stream().map(VocabularyTerm::getName).sorted().collect(Collectors.joining("_"));
				}))));
			groupedDAs.forEach((disease, relationListMap) -> relationListMap.forEach((relation, daList1) -> {
				daList1.forEach((s, daList) -> {
					Set<Gene> geneList = daList.stream().map(DiseaseAnnotation::getWith).flatMap(Collection::stream).collect(Collectors.toSet());
					daList.forEach(diseaseAnnotation -> diseaseAnnotation.setWith(new ArrayList<>(geneList)));
				});

			}));
		});
		log.info("Number of orthologous genes generating new DAs: " + newDAMap.size());
		
		writeToCache(orthoCacheFileName, newDAMap);
		
		return newDAMap;
	}
}
