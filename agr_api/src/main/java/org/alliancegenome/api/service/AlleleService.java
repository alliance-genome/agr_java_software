package org.alliancegenome.api.service;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.cache.repository.AlleleCacheRepository;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;

@RequestScoped
public class AlleleService {

    private AlleleRepository alleleRepo = new AlleleRepository();
    private AlleleCacheRepository alleleCacheRepo = new AlleleCacheRepository();

    public Allele getById(String id) {
        return alleleRepo.getAllele(id);
    }

    public JsonResultResponse<Allele> getAllelesBySpecies(String species, Pagination pagination) {
        String taxon = SpeciesType.getTaxonId(species);
        return alleleCacheRepo.getAllelesBySpecies(taxon, pagination);
    }

    public JsonResultResponse<Allele> getAllelesByGene(String geneID, Pagination pagination) {
        return alleleCacheRepo.getAllelesByGene(geneID, pagination);
    }


}
