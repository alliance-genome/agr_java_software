package org.alliancegenome.api.service.helper;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.alliancegenome.api.service.EntityType;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class APIServiceHelper {

	private static GeneRepository repository = new GeneRepository();
	private static DiseaseRepository diseaseRepository = new DiseaseRepository();
	private static AlleleRepository alleleRepository = new AlleleRepository();
	
	private APIServiceHelper() {} // All Static Methods
	
	public static String getFileName(String title, String id, EntityType collectionType, String extra) {
		String fileName = title;
		fileName += "-";
		fileName += id;
		fileName += "-";
		// make the entity name plural
		fileName += collectionType.toString().toLowerCase() + "s";
		fileName += "-";
		if(extra != null && extra.length() > 0) {
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
				if (gene != null)
					entityName = gene.getSymbol();
				break;
			case DISEASE:
				DOTerm disease = diseaseRepository.getDiseaseTerm(id);
				if (disease != null)
					entityName = disease.getName();
				break;
			case ALLELE:
				Allele allele = alleleRepository.getAllele(id);
				if (allele != null)
					entityName = allele.getSymbol();
				break;
			default:
		}
		return entityName;
	}
}
