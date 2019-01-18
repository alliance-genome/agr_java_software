package org.alliancegenome.api.service;

import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.alliancegenome.es.util.SearchHitIterator;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;

import javax.enterprise.context.RequestScoped;
import java.util.List;

@RequestScoped
public class DiseaseService {

    private static DiseaseRepository diseaseRepository= new DiseaseRepository();

    public DOTerm getById(String id) {
        return diseaseRepository.getDiseaseTerm(id);
    }

    public List<DiseaseAnnotation> getDiseaseAnnotations(String id, Pagination pagination) {
        //return diseaseRepository.getDiseaseAssociation(id, pagination);
        return null;
    }


    public SearchHitIterator getDiseaseAnnotationsDownload(String id, Pagination pagination) {
        ///return diseaseRepository.getDiseaseAnnotationsDownload(id, pagination);
        return null;
    }
}
