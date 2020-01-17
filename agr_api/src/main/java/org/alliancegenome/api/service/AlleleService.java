package org.alliancegenome.api.service;

import org.alliancegenome.cache.repository.AlleleCacheRepository;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.repository.AlleleRepository;

import javax.enterprise.context.RequestScoped;
import java.time.LocalDateTime;
import java.util.List;

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


    public JsonResultResponse<Variant> getVariants(String id, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();

        List<Variant> variants = alleleRepo.getVariants(id);

        JsonResultResponse<Variant> result = new JsonResultResponse<>();

        FilterService<Variant> filterService = new FilterService<>(null);
        result.setTotal(variants.size());
        result.setResults(filterService.getPaginatedAnnotations(pagination, variants));
        result.calculateRequestDuration(startDate);
        return result;
    }

    public JsonResultResponse<PhenotypeAnnotation> getPhenotype(String id, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();

        List<PhenotypeAnnotation> variants = alleleCacheRepo.getPhenotype(id);

        JsonResultResponse<PhenotypeAnnotation> result = new JsonResultResponse<>();

        FilterService<PhenotypeAnnotation> filterService = new FilterService<>(null);
        result.setTotal(variants.size());
        result.setResults(filterService.getPaginatedAnnotations(pagination, variants));
        result.calculateRequestDuration(startDate);
        return result;
    }
}
