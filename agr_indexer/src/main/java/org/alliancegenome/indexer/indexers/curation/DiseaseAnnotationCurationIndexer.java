package org.alliancegenome.indexer.indexers.curation;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
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

	protected void createJsonFile(Map<String, Pair<Gene, ArrayList<DiseaseAnnotation>>> geneMap, String fileName) {
		RestDefaultObjectMapper restDefaultObjectMapper = new RestDefaultObjectMapper();
		ObjectMapper mapper = restDefaultObjectMapper.getMapper();
		mapper.writerWithView(View.FieldsAndLists.class);

		try (PrintStream out = new PrintStream(new FileOutputStream(fileName))) {
			out.print(mapper.writeValueAsString(geneMap));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {

	}

	@Override
	protected void index() {
		indexGenes();
		indexAlleles();
		indexAGMs();

		createJsonFile(geneMap, "geneMap.json");
		
		//createGeneDiseaseAnnotationDocuments();
		//createAlleleDiseaseAnnotationDocuments();
		//createAGMDiseaseAnnotationDocuments();

	}

	private void indexGenes() {

		List<GeneDiseaseAnnotation> geneDiseaseAnnotations = geneService.getAll();

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

		List<AlleleDiseaseAnnotation> alleleDiseaseAnnotations = alleleService.getAll();

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

		List<AGMDiseaseAnnotation> agmDiseaseAnnotations = agmService.getAll();

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
