package org.alliancegenome.api.service.helper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.alliancegenome.api.service.EntityType;
import org.alliancegenome.core.helpers.DiseaseAnnotationHelper;
import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.BiologicalEntity;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;

import io.quarkus.logging.Log;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class APIServiceHelper {

	private static GeneRepository repository = new GeneRepository();
	private static DiseaseRepository diseaseRepository = new DiseaseRepository();
	private static AlleleRepository alleleRepository = new AlleleRepository();
	
	private APIServiceHelper() { } // All Static Methods
	
	public static String getFileName(String title, String id, EntityType collectionType, String extra) {
		String fileName = title;
		fileName += "-";
		fileName += id;
		fileName += "-";
		// make the entity name plural
		fileName += collectionType.toString().toLowerCase() + "s";
		fileName += "-";
		if (extra != null && extra.length() > 0) {
			fileName += extra;
			fileName += "-";
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		fileName += dateFormat.format(new Date());
		fileName += ".tsv";
		return fileName;
	}

	public static void setDownloadHeader(String id, EntityType type, EntityType collectionType, Response.ResponseBuilder responseBuilder) {
		setDownloadHeader(id, type, collectionType, "", responseBuilder);
	}
	
	public static void setDownloadHeader(String id, EntityType type, EntityType collectionType, String extraFileName, Response.ResponseBuilder responseBuilder) {
		String title = getEntityName(id, type);
		String fileName = APIServiceHelper.getFileName(title, id, collectionType, extraFileName);
		responseBuilder.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
	}

	/**
	 * Retrieve the name / symbol of an entity given by an ID
	 *
	 * @param id id of entity
	 * @return name of entity
	 */
	public static String getEntityName(String id, EntityType type) {
		String entityName = "NotFound";
		switch (type) {
			case GENE:
				Gene gene = repository.getShallowGene(id);
				if (gene != null) {
					entityName = gene.getSymbol();
				}
				break;
			case DISEASE:
				DOTerm disease = diseaseRepository.getDiseaseTerm(id);
				if (disease != null) {
					entityName = disease.getName();
				}
				break;
			case ALLELE:
				Allele allele = alleleRepository.getAllele(id);
				if (allele != null) {
					entityName = allele.getSymbol();
				}
				break;
			default:
		}
		return entityName;
	}
	
	//copied from natural sort used in the frontend

	/**
	 * implements a natural sort
	 * 
	 * @param <T> the type of objects being compared
	 * @param accessor a function that extracts the string value to sort by from objects of type T
	 * @return a Comparator that performs natural sorting
	 * @see <a href="https://wikipedia.org/wiki/Natural_sort_order">Natural sort order</a>
	 */
	public static <T> Comparator<T> smartAlphaSort(Function<T, String> accessor) {
		return (a, b) -> {
			String ax = accessor.apply(a).toLowerCase();
			String bx = accessor.apply(b).toLowerCase();
			
			// Split strings into chunks of strings and numbers
			Pattern splitRegex = Pattern.compile("([0-9]+|[^0-9]+)");
			List<String> aChunksArray = extractChunks(ax, splitRegex);
			List<String> bChunksArray = extractChunks(bx, splitRegex);
			
			int len = Math.min(aChunksArray.size(), bChunksArray.size());
			
			for (int i = 0; i < len; i++) {
				String aChunk = aChunksArray.get(i);
				String bChunk = bChunksArray.get(i);
				
				// If both parts are numeric, compare as numbers
				if (isNumeric(aChunk) && isNumeric(bChunk)) {
					int diff = Integer.parseInt(aChunk) - Integer.parseInt(bChunk);
					if (diff != 0) return diff;
				}
				// Otherwise compare as strings
				else {
					int diff = aChunk.compareToIgnoreCase(bChunk);
					if (diff != 0) return diff;
				}
			}
			
			// If all parts are equal up to the length of the shorter string,
			// the shorter string comes first
			return aChunksArray.size() - bChunksArray.size();
		};
	}
	
	private static List<String> extractChunks(String input, Pattern pattern) {
		List<String> chunks = new ArrayList<>();
		Matcher matcher = pattern.matcher(input);
		while (matcher.find()) {
			chunks.add(matcher.group());
		}
		return chunks;
	}
	
	private static boolean isNumeric(String str) {
		if (str == null || str.isEmpty()) {
			return false;
		}
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
	 * Takes a list of disease annotations and returns the list naturally sorted 
	 * by the annotation subject symbol/name.
	 * 
	 * @param annotations the list of annotations to sort
	 * @return a new list with annotations sorted naturally by subject text
	 */
	public static List<DiseaseAnnotation> naturalSortByAnnotationSubject(List<DiseaseAnnotation> annotations) {
		return annotations.stream()
			.sorted(smartAlphaSort(APIServiceHelper::getAnnotationSubjectText))
			.collect(Collectors.toList());
	}
	
	private static String getAnnotationSubjectText(DiseaseAnnotation annotation) {
		if (annotation == null) {
			return "";
		}
		
		//can't get subject from DiseaseAnnotation directly, so check the specific types
		try {
			BiologicalEntity subject = null;
			if (annotation instanceof GeneDiseaseAnnotation gda) {
				subject = gda.getDiseaseAnnotationSubject();
			} else if (annotation instanceof AlleleDiseaseAnnotation ada) {
				subject = ada.getDiseaseAnnotationSubject();
			} else if (annotation instanceof AGMDiseaseAnnotation agmda) {
				subject = agmda.getDiseaseAnnotationSubject();
			}
			
			return subject != null ? DiseaseAnnotationHelper.getEntityName(subject) : "";
		} catch (Exception e) {
			Log.error(e);
			return "";
		}
	}
}
