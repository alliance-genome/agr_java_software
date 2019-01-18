package org.alliancegenome.api.controller;

import org.alliancegenome.api.rest.interfaces.DiseaseRESTInterface;
import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.DOTerm;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@RequestScoped
public class DiseaseController extends BaseController implements DiseaseRESTInterface {

    //private final Logger log = Logger.getLogger(getClass());
    @Context  //injected response proxy supporting multiple threads
    private HttpServletResponse response;

    @Inject
    private DiseaseService diseaseService;
    private final DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();


    @Override
    public DOTerm getDisease(String id) {
        DOTerm doTerm = diseaseService.getById(id);
        if (doTerm == null) {
            throw new NotFoundException();
        } else {
            return doTerm;
        }
    }

    @Override
    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsSorted(String id,
                                                                             int limit,
                                                                             int page,
                                                                             String sortBy,
                                                                             String geneName,
                                                                             String species,
                                                                             String geneticEntity,
                                                                             String geneticEntityType,
                                                                             String disease,
                                                                             String source,
                                                                             String reference,
                                                                             String evidenceCode,
                                                                             String associationType,
                                                                             String asc) {
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        pagination.addFieldFilter(FieldFilter.GENE_NAME, geneName);
        pagination.addFieldFilter(FieldFilter.SPECIES, species);
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY, geneticEntity);
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityType);
        pagination.addFieldFilter(FieldFilter.DISEASE, disease);
        pagination.addFieldFilter(FieldFilter.SOURCE, source);
        pagination.addFieldFilter(FieldFilter.REFERENCE, reference);
        pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, evidenceCode);
        pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, associationType);
        if (pagination.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            try {
                response.flushBuffer();
            } catch (Exception ignored) {
                // handle error
            }
        }
        return diseaseService.getDiseaseAnnotations(id, pagination);
    }

    @Override
    public Response getDiseaseAnnotationsDownloadFile(String id) {

        Response.ResponseBuilder response = Response.ok(getDiseaseAnnotationsDownload(id));
        response.type(MediaType.TEXT_PLAIN_TYPE);
        response.header("Content-Disposition", "attachment; filename=\"disease-annotations-" + id.replace(":", "-") + ".tsv\"");
        return response.build();
    }

    @Override
    public String getDiseaseAnnotationsDownload(String id) {
        Pagination pagination = new Pagination(1, Integer.MAX_VALUE, null, null);
        // retrieve all records
        return translator.getAllRows(diseaseService.getDiseaseAnnotationsDownload(id, pagination));
    }

}
