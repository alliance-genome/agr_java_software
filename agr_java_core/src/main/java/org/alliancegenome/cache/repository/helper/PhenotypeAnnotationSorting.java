package org.alliancegenome.cache.repository.helper;

import java.util.*;

import org.alliancegenome.neo4j.entity.*;
import org.alliancegenome.neo4j.entity.node.Allele;

public class PhenotypeAnnotationSorting implements Sorting<PhenotypeAnnotation> {

    private List<Comparator<PhenotypeAnnotation>> defaultList;
    private List<Comparator<PhenotypeAnnotation>> geneticEntityList;

    private static Comparator<PhenotypeAnnotation> phenotypeOrder =
            Comparator.comparing(annotation -> annotation.getPhenotype().toLowerCase());

    private static Comparator<PhenotypeAnnotation> geneticEntityOrder =
            Comparator.comparing(annotation -> {
                        Allele allele = annotation.getAllele();
                        // sort gene records without alleles after alleles
                        if (allele == null)
                            return "zzz";
                        // create alpha-smart key
                        String smartSymbol = Sorting.getSmartKey(allele.getSymbol());
                        return smartSymbol;
                    }
            );


    private static Comparator<PhenotypeAnnotation> geneticEntityTypeOrder =
            Comparator.comparing(annotation -> annotation.getAllele().getType());


    public PhenotypeAnnotationSorting() {
        super();

        defaultList = new ArrayList<>(2);
        defaultList.add(phenotypeOrder);
        defaultList.add(geneticEntityOrder);

        geneticEntityList = new ArrayList<>(2);
        geneticEntityList.add(geneticEntityOrder);
        geneticEntityList.add(phenotypeOrder);
    }

    public Comparator<PhenotypeAnnotation> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getJoinedComparator(defaultList);

        switch (field) {
            case PHENOTYPE:
                return getJoinedComparator(defaultList);
            case GENETIC_ENTITY:
                return getJoinedComparator(geneticEntityList);
            default:
                return getJoinedComparator(defaultList);
        }
    }

}
