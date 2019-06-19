package org.alliancegenome.api.service;

import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.Date;

public class APIService {

    public static String getFileName(String title, String id, EntityType collectionType) {
        String fileName = title;
        fileName += "-";
        fileName += id;
        fileName += "-";
        // make the entity name plural
        fileName += collectionType.toString().toLowerCase() + "s";
        fileName += "-";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        fileName += dateFormat.format(new Date());
        fileName += ".tsv";
        return fileName;
    }

    public static void setDownloadHeader(String id, EntityType type, EntityType collectionType, Response.ResponseBuilder responseBuilder) {
        String title = getEntityName(id, type);
        String fileName = APIService.getFileName(title, id, collectionType);
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
                GeneRepository repository = new GeneRepository();
                Gene gene = repository.getShallowGene(id);
                if (gene != null)
                    entityName = gene.getSymbol();
                break;
            case DISEASE:
                DiseaseRepository diseaseRepository = new DiseaseRepository();
                DOTerm disease = diseaseRepository.getDiseaseTerm(id);
                if (disease != null)
                    entityName = disease.getName();
                break;
            default:
        }
        return entityName;
    }
}
