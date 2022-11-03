package org.alliancegenome.indexer.indexers.curation;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class DiseaseAnnotationCurationIndexer extends Indexer<SearchableItemDocument> {

	public DiseaseAnnotationCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	protected void createDA(DiseaseAnnotation diseaseAnnotation, DiseaseAnnotation newDiseaseAnnotation) {
		newDiseaseAnnotation.setAnnotationType(diseaseAnnotation.getAnnotationType());
		newDiseaseAnnotation.setDiseaseQualifiers(diseaseAnnotation.getDiseaseQualifiers());
		newDiseaseAnnotation.setDiseaseGeneticModifier(diseaseAnnotation.getDiseaseGeneticModifier());
		newDiseaseAnnotation.setConditionRelations(diseaseAnnotation.getConditionRelations());
		newDiseaseAnnotation.setDataProvider(diseaseAnnotation.getDataProvider());
		newDiseaseAnnotation.setDiseaseGeneticModifierRelation(diseaseAnnotation.getDiseaseGeneticModifierRelation());
		newDiseaseAnnotation.setEvidenceCodes(diseaseAnnotation.getEvidenceCodes());
		newDiseaseAnnotation.setGeneticSex(diseaseAnnotation.getGeneticSex());
		newDiseaseAnnotation.setDiseaseRelation(diseaseAnnotation.getDiseaseRelation());
		newDiseaseAnnotation.setNegated(diseaseAnnotation.getNegated());
		newDiseaseAnnotation.setRelatedNotes(diseaseAnnotation.getRelatedNotes());
		newDiseaseAnnotation.setObject(diseaseAnnotation.getObject());
		newDiseaseAnnotation.setSecondaryDataProvider(diseaseAnnotation.getSecondaryDataProvider());
		newDiseaseAnnotation.setSingleReference(diseaseAnnotation.getSingleReference());
		newDiseaseAnnotation.setWith(diseaseAnnotation.getWith());
		newDiseaseAnnotation.setDateCreated(diseaseAnnotation.getDateCreated());
		newDiseaseAnnotation.setDateUpdated(diseaseAnnotation.getDateUpdated());
		newDiseaseAnnotation.setInternal(diseaseAnnotation.getInternal());
		newDiseaseAnnotation.setObsolete(diseaseAnnotation.getObsolete());
		newDiseaseAnnotation.setUpdatedBy(diseaseAnnotation.getUpdatedBy());
	}
	
	protected List<AlleleDiseaseAnnotation> expandAlleleDiseaseAnnotationsFromAGMDiseaseAnnotations(List<AGMDiseaseAnnotation> agmDiseaseAnnotations) {
		
		List<AlleleDiseaseAnnotation> alleleDiseaseAnnotations = new ArrayList<>();
		agmDiseaseAnnotations.forEach(agmDiseaseAnnotation -> {
			if (agmDiseaseAnnotation.getInferredAllele() != null && !agmDiseaseAnnotation.getInferredAllele().getInternal()) {
				AlleleDiseaseAnnotation alleleDiseaseAnnotation = new AlleleDiseaseAnnotation();
				createDA(agmDiseaseAnnotation, alleleDiseaseAnnotation);
				alleleDiseaseAnnotation.setSubject(agmDiseaseAnnotation.getInferredAllele());
				alleleDiseaseAnnotations.add(alleleDiseaseAnnotation);
			}
			if (agmDiseaseAnnotation.getAssertedAllele() != null && !agmDiseaseAnnotation.getAssertedAllele().getInternal()) {
				AlleleDiseaseAnnotation alleleDiseaseAnnotation = new AlleleDiseaseAnnotation();
				createDA(agmDiseaseAnnotation, alleleDiseaseAnnotation);
				alleleDiseaseAnnotation.setSubject(agmDiseaseAnnotation.getAssertedAllele());
				alleleDiseaseAnnotations.add(alleleDiseaseAnnotation);
			}
		});
		return alleleDiseaseAnnotations;
	}

	protected List<GeneDiseaseAnnotation> expandGeneDiseaseAnnotationsFromAGMDiseaseAnnotations(List<AGMDiseaseAnnotation> agmDiseaseAnnotations) {
		List<GeneDiseaseAnnotation> geneDiseaseAnnotations = new ArrayList<>();
		agmDiseaseAnnotations.forEach(agmDiseaseAnnotation -> {
			if (agmDiseaseAnnotation.getInferredGene() != null && !agmDiseaseAnnotation.getInferredGene().getInternal()) {
				GeneDiseaseAnnotation geneAnnotation = new GeneDiseaseAnnotation();
				createDA(agmDiseaseAnnotation, geneAnnotation);
				geneAnnotation.setSubject(agmDiseaseAnnotation.getInferredGene());
				geneDiseaseAnnotations.add(geneAnnotation);
			}
			if (agmDiseaseAnnotation.getAssertedGenes() != null) {
				for(Gene gene: agmDiseaseAnnotation.getAssertedGenes()) {
					if(!gene.getInternal()) {
						GeneDiseaseAnnotation geneDiseaseAnnotation = new GeneDiseaseAnnotation();
						createDA(agmDiseaseAnnotation, geneDiseaseAnnotation);
						geneDiseaseAnnotation.setSubject(gene);
						geneDiseaseAnnotations.add(geneDiseaseAnnotation);
					}
				}
			}
		});

		return geneDiseaseAnnotations;
	}
	
	protected List<GeneDiseaseAnnotation> expandGeneAnnotationsFromAlleleDiseaseAnnotations(List<AlleleDiseaseAnnotation> annotations) {
		List<GeneDiseaseAnnotation> geneDiseaseAnnotations = new ArrayList<>();
		
		annotations.forEach(alleleDiseaseAnnotation -> {
			if (alleleDiseaseAnnotation.getInferredGene() != null && !alleleDiseaseAnnotation.getInferredGene().getInternal()) {
				GeneDiseaseAnnotation geneDiseaseAnnotation = new GeneDiseaseAnnotation();
				createDA(alleleDiseaseAnnotation, geneDiseaseAnnotation);
				geneDiseaseAnnotation.setSubject(alleleDiseaseAnnotation.getInferredGene());
				geneDiseaseAnnotations.add(geneDiseaseAnnotation);
			}
			if (alleleDiseaseAnnotation.getAssertedGenes() != null) {
				for(Gene gene: alleleDiseaseAnnotation.getAssertedGenes()) {
					if(!gene.getInternal()) {
						GeneDiseaseAnnotation geneDiseaseAnnotation = new GeneDiseaseAnnotation();
						createDA(alleleDiseaseAnnotation, geneDiseaseAnnotation);
						geneDiseaseAnnotation.setSubject(gene);
						geneDiseaseAnnotations.add(geneDiseaseAnnotation);
					}
				}
			}
		});
		return geneDiseaseAnnotations;
	}
	
	protected <D extends DiseaseAnnotation> void createJsonFile(List<D> annotations, String fileName) {
		RestDefaultObjectMapper restDefaultObjectMapper = new RestDefaultObjectMapper();
		ObjectMapper mapper = restDefaultObjectMapper.getMapper();
		mapper.writerWithView(View.FieldsAndLists.class);
		ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
		String jsonInString = null;
		log.info("Writting output file: " + fileName);
		try (PrintStream out = new PrintStream(new FileOutputStream(fileName))) {
			jsonInString = writer.writeValueAsString(annotations);
			out.print(jsonInString);
		} catch (FileNotFoundException | JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		log.info("Output files finished");
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {

	}

}
