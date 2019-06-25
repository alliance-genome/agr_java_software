package org.alliancegenome.cacher.cachers.db;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alliancegenome.cacher.cachers.DBCacher;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.PhenotypeEntityJoin;
import org.alliancegenome.neo4j.repository.PhenotypeRepository;

public class GenePhenotypeDBCacher extends DBCacher<List<PhenotypeAnnotation>> {
    
    private static PhenotypeRepository phenotypeRepository = new PhenotypeRepository();
    
    public GenePhenotypeDBCacher(String cacheName) {
        super(cacheName);
    }

    @Override
    protected void cache() {

        List<PhenotypeEntityJoin> joinList = phenotypeRepository.getAllPhenotypeAnnotations();
        int size = joinList.size();
        DecimalFormat myFormatter = new DecimalFormat("###,###.##");
        System.out.println("Retrieved " + myFormatter.format(size) + " phenotype records");
        // replace Gene references with the cached Gene references to keep the memory imprint low.
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
                    return document;
                })
                .collect(toList());

        // group by gene IDs
        Map<String, List<PhenotypeAnnotation>> phenotypeAnnotationMap = allPhenotypeAnnotations.stream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGene().getPrimaryKey()));

        for(Entry<String, List<PhenotypeAnnotation>> entry: phenotypeAnnotationMap.entrySet()) {
            cache.put(entry.getKey(), entry.getValue());
        }
        
    }

}
