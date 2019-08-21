package org.alliancegenome.cacher.cachers;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.PhenotypeCacheManager;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.PhenotypeEntityJoin;
import org.alliancegenome.neo4j.repository.PhenotypeRepository;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class GenePhenotypeCacher extends Cacher {

    private static PhenotypeRepository phenotypeRepository = new PhenotypeRepository();

    public GenePhenotypeCacher() {
        super();
    }

    @Override
    protected void cache() {

        List<PhenotypeEntityJoin> joinList = phenotypeRepository.getAllPhenotypeAnnotations();
        int size = joinList.size();
        DecimalFormat myFormatter = new DecimalFormat("###,###.##");
        log.info("Retrieved " + myFormatter.format(size) + " phenotype records");
        // replace Gene references with the cached Gene references to keep the memory imprint low.
        startProcess("allPhenotypeAnnotations", size);
        
        List<PhenotypeAnnotation> allPhenotypeAnnotations = joinList.stream()
                .map(phenotypeEntityJoin -> {
                    PhenotypeAnnotation document = new PhenotypeAnnotation();
                    document.setGene(phenotypeEntityJoin.getGene());
                    if (phenotypeEntityJoin.getAllele() != null)
                        document.setGeneticEntity(phenotypeEntityJoin.getAllele());
                    else
                        document.setGeneticEntity(phenotypeEntityJoin.getGene());
                    document.setPhenotype(phenotypeEntityJoin.getPhenotype().getPhenotypeStatement());
                    document.setPublications(phenotypeEntityJoin.getPublications());
                    progress();
                    return document;
                })
                .collect(toList());

        finishProcess();
        
        // group by gene IDs
        Map<String, List<PhenotypeAnnotation>> phenotypeAnnotationMap = allPhenotypeAnnotations.stream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGene().getPrimaryKey()));

        PhenotypeCacheManager manager = new PhenotypeCacheManager();
        
        startProcess("phenotypeAnnotationMap into cache", phenotypeAnnotationMap.size());
        
        phenotypeAnnotationMap.forEach((key, value) -> {
            JsonResultResponse<PhenotypeAnnotation> result = new JsonResultResponse<>();
            result.setResults(value);
            try {
                manager.putCache(key, result, View.PhenotypeAPI.class, CacheAlliance.PHENOTYPE);
                progress();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        finishProcess();
    }


}
