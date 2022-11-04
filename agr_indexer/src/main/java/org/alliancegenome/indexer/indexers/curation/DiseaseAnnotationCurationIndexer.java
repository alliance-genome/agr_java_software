package org.alliancegenome.indexer.indexers.curation;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.curation_api.config.RestDefaultObjectMapper;
import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.view.View;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.document.GeneDiseaseAnnotationDocument;
import org.alliancegenome.indexer.indexers.curation.service.AGMDiseaseAnnotationService;
import org.alliancegenome.indexer.indexers.curation.service.AlleleDiseaseAnnotationService;
import org.alliancegenome.indexer.indexers.curation.service.GeneDiseaseAnnotationService;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class DiseaseAnnotationCurationIndexer extends Indexer<SearchableItemDocument> {

	private GeneDiseaseAnnotationService geneService = new GeneDiseaseAnnotationService();
	private AlleleDiseaseAnnotationService alleleService = new AlleleDiseaseAnnotationService();
	private AGMDiseaseAnnotationService agmService = new AGMDiseaseAnnotationService();


	private Map<String, Pair<Gene, ArrayList<DiseaseAnnotation>>> geneMap = new HashMap<String, Pair<Gene, ArrayList<DiseaseAnnotation>>>();

	// TODO implement these in the future when we switch to the other tables.
	//private Map<String, Pair<Allele, ArrayList<DiseaseAnnotation>>> alleleMap = new HashMap<String, Pair<Allele, ArrayList<DiseaseAnnotation>>>();
	//private Map<String, Pair<AffectedGenomicModel, ArrayList<DiseaseAnnotation>>> agmMap = new HashMap<String, Pair<AffectedGenomicModel, ArrayList<DiseaseAnnotation>>>();


	public DiseaseAnnotationCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	protected void createJsonFile(List<GeneDiseaseAnnotationDocument> gdaList, String fileName) {
		RestDefaultObjectMapper restDefaultObjectMapper = new RestDefaultObjectMapper();
		ObjectMapper mapper = restDefaultObjectMapper.getMapper();
		mapper.writerWithView(View.FieldsAndLists.class);
		ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
		try (PrintStream out = new PrintStream(new FileOutputStream(fileName))) {
			out.print(writer.writeValueAsString(gdaList));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {

	}

	@Override
	protected void index() {
		//indexGenes();
		indexAlleles();
		//indexAGMs();

		List<GeneDiseaseAnnotationDocument> list = createGeneDiseaseAnnotationDocuments();
		
		createJsonFile(list, "gdaList.json");
		
		//createAlleleDiseaseAnnotationDocuments();
		//createAGMDiseaseAnnotationDocuments();

	}

	private List<GeneDiseaseAnnotationDocument> createGeneDiseaseAnnotationDocuments() {

		List<GeneDiseaseAnnotationDocument> ret = new ArrayList<>();
		
		for(Entry<String, Pair<Gene, ArrayList<DiseaseAnnotation>>> entry: geneMap.entrySet()) {
			HashMap<String, GeneDiseaseAnnotationDocument> lookup = new HashMap<>();
			
			for(DiseaseAnnotation da: entry.getValue().getRight()) {
				String key = da.getDiseaseRelation().getName() + "_" + da.getObject().getName();
				System.out.println(key);
				GeneDiseaseAnnotationDocument gda = lookup.get(key);
				
				if(gda == null) {
					gda = new GeneDiseaseAnnotationDocument();
					gda.setSubject(entry.getValue().getLeft());
					gda.setDiseaseRelation(da.getDiseaseRelation());
					gda.setObject(da.getObject());
					List<DiseaseAnnotation> list = new ArrayList<>();
					list.add(da);
					gda.setPrimaryAnnotations(list);
					//gda.setEvidenceCodes(da.getEvidenceCodes());
					//gda.setDataProvider(da.getDataProvider());
					//gda.setSingleReference(da.getSingleReference());
					lookup.put(key, gda);
				} else {
					gda.getPrimaryAnnotations().add(da);
				}
			}
			ret.addAll(lookup.values());
			lookup.clear();
		}
		
		return ret;
	}

	private void indexGenes() {

		List<GeneDiseaseAnnotation> geneDiseaseAnnotations = geneService.getFiltered();

		for(GeneDiseaseAnnotation da: geneDiseaseAnnotations) {
			Pair<Gene, ArrayList<DiseaseAnnotation>> pair = geneMap.get(da.getSubject().getCurie());
			if(pair == null) {
				pair = Pair.of(da.getSubject(), new ArrayList<DiseaseAnnotation>());
				geneMap.put(da.getSubject().getCurie(), pair);
			}
			pair.getRight().add(da);
		}

	}

	private void indexAlleles() {

		List<AlleleDiseaseAnnotation> alleleDiseaseAnnotations = alleleService.getFiltered();

		for(AlleleDiseaseAnnotation da: alleleDiseaseAnnotations) {

			if(da.getInferredGene() != null) {
				Pair<Gene, ArrayList<DiseaseAnnotation>> pair = geneMap.get(da.getInferredGene().getCurie());
				if(pair == null) {
					pair = Pair.of(da.getInferredGene(), new ArrayList<DiseaseAnnotation>());
					geneMap.put(da.getInferredGene().getCurie(), pair);
				}
				pair.getRight().add(da);
			}

			if(da.getAssertedGenes() != null) {
				for(Gene gene: da.getAssertedGenes()) {
					Pair<Gene, ArrayList<DiseaseAnnotation>> pair = geneMap.get(gene.getCurie());
					if(pair == null) {
						pair = Pair.of(gene, new ArrayList<DiseaseAnnotation>());
						geneMap.put(gene.getCurie(), pair);
					}
					pair.getRight().add(da);
				}
			}
		}

	}

	private void indexAGMs() {

		List<AGMDiseaseAnnotation> agmDiseaseAnnotations = agmService.getFiltered();

		for(AGMDiseaseAnnotation da: agmDiseaseAnnotations) {

			if(da.getInferredGene() != null) {
				Pair<Gene, ArrayList<DiseaseAnnotation>> pair = geneMap.get(da.getInferredGene().getCurie());
				if(pair == null) {
					pair = Pair.of(da.getInferredGene(), new ArrayList<DiseaseAnnotation>());
					geneMap.put(da.getInferredGene().getCurie(), pair);
				}
				pair.getRight().add(da);
			}

			if(da.getAssertedGenes() != null) {
				for(Gene gene: da.getAssertedGenes()) {
					Pair<Gene, ArrayList<DiseaseAnnotation>> pair = geneMap.get(gene.getCurie());

					if(pair == null) {
						pair = Pair.of(gene, new ArrayList<DiseaseAnnotation>());
						geneMap.put(gene.getCurie(), pair);
					}
					pair.getRight().add(da);
				}
			}
		}

	}

	public static void main(String[] args) {
		DiseaseAnnotationCurationIndexer indexer = new DiseaseAnnotationCurationIndexer(IndexerConfig.DiseaseAnnotationMlIndexer);
		indexer.index();
		System.exit(0);
	}


}
