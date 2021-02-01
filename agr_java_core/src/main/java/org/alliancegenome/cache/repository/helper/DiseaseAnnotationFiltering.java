package org.alliancegenome.cache.repository.helper;

import static org.alliancegenome.neo4j.entity.DiseaseAnnotation.NOT_ASSOCIATION_TYPE;
import static org.alliancegenome.neo4j.entity.SpeciesType.NCBITAXON;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.apache.commons.lang3.StringUtils;

public class DiseaseAnnotationFiltering extends AnnotationFiltering<DiseaseAnnotation> {


    public FilterFunction<DiseaseAnnotation, String> termNameFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getDisease().getName(), value);

    public FilterFunction<DiseaseAnnotation, String> alleleFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getFeature().getSymbolText(), value);

    public FilterFunction<DiseaseAnnotation, String> geneticEntityFilter =
            (annotation, value) -> {
                if (annotation.getFeature() == null)
                    return false;
                return FilterFunction.contains(annotation.getFeature().getSymbolText(), value);
            };

    public FilterFunction<DiseaseAnnotation, String> associationFilter =
            (annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getAssociationType(), value);

    public FilterFunction<DiseaseAnnotation, String> sourceFilter =
            (annotation, value) -> {
                if (annotation.getProviders() == null)
                    return FilterFunction.contains(annotation.getSource().getName(), value);
                return FilterFunction.contains(annotation.getProviders().stream()
                        .map(Map::values)
                        .flatMap(Collection::stream)
                        .map(CrossReference::getDisplayName).collect(Collectors.joining(",")), value);
            };

    public FilterFunction<DiseaseAnnotation, String> geneticEntityTypeFilter =
            (annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getGeneticEntityType(), value);

    public FilterFunction<DiseaseAnnotation, String> geneNameFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getGene().getSymbol(), value);

    public FilterFunction<DiseaseAnnotation, String> geneSpeciesFilter =
            (annotation, value) -> {
                if (annotation.getGene() != null) {
                    if (value.startsWith(NCBITAXON))
                        return FilterFunction.fullMatchMultiValueOR(annotation.getGene().getSpecies().getType().getTaxonID(), value);
                    else
                        return FilterFunction.fullMatchMultiValueOR(annotation.getGene().getSpecies().getName(), value);
                }
                if (annotation.getFeature() != null) {
                    if (value.startsWith(NCBITAXON))
                        return FilterFunction.fullMatchMultiValueOR(annotation.getFeature().getSpecies().getType().getTaxonID(), value);
                    else
                        return FilterFunction.fullMatchMultiValueOR(annotation.getFeature().getSpecies().getName(), value);
                }
                return false;
            };

    public FilterFunction<DiseaseAnnotation, String> evidenceCodeFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getEcoCodes().stream()
                        // this encodes the logic:
                        // if there is a displaySynonym (three / four-letter abbrev then check that attribute
                        // otherwise check the term name
                        .map(evidenceCode -> {
                            if (StringUtils.isNotEmpty(evidenceCode.getDisplaySynonym()))
                                return FilterFunction.contains(evidenceCode.getDisplaySynonym(), value);
                            else
                                return FilterFunction.contains(evidenceCode.getName(), value);
                        })
                        .collect(Collectors.toSet());
                return filteringPassed.contains(true);
            };

    public FilterFunction<DiseaseAnnotation, String> basedOnGeneFilter =
            (annotation, value) -> {
                if (annotation.getOrthologyGenes() == null)
                    return false;
                StringBuilder fullGeneSpeciesName = new StringBuilder();

                annotation.getOrthologyGenes().forEach(gene -> {
                    String fullName = gene.getSymbol();
                    final String primaryKey = gene.getSpecies().getPrimaryKey();
                    SpeciesType speciesType = SpeciesType.getTypeByID(primaryKey);
                    if (speciesType == null)
                        throw new RuntimeException("No Species found for " + primaryKey);
                    fullName += " (" + speciesType.getAbbreviation() + ") ";
                    fullGeneSpeciesName.append(fullName);
                });
                return FilterFunction.contains(fullGeneSpeciesName.toString(), value);
            };

    public FilterFunction<DiseaseAnnotation, String> includeNegationFilter =
            (annotation, value) -> {
                if (annotation.getAssociationType() == null)
                    return true;
                if (value != null && value.equalsIgnoreCase("true"))
                    return true;
                return !annotation.getAssociationType().toLowerCase().contains(NOT_ASSOCIATION_TYPE);
            };

    public FilterFunction<DiseaseAnnotation, String> referenceFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getPublications().stream()
                        .map(publication -> FilterFunction.contains(publication.getPubId(), value))
                        .collect(Collectors.toSet());
                // return true if at least one pub is found
                return filteringPassed.contains(true);
            };

    public FilterFunction<DiseaseAnnotation, String> orthologFilter =
            (annotation, value) -> {
                Gene orthologyGene = annotation.getOrthologyGene();
                if (orthologyGene == null)
                    return false;
                return FilterFunction.contains(orthologyGene.getSymbol(), value);
            };

    public FilterFunction<DiseaseAnnotation, String> orthologSpeciesFilter =
            (annotation, value) -> {
                Gene orthologyGene = annotation.getOrthologyGene();
                if (orthologyGene == null)
                    return false;
                return FilterFunction.contains(orthologyGene.getSpecies().getName(), value);
            };


    public DiseaseAnnotationFiltering() {
        filterFieldMap.put(FieldFilter.ALLELE, alleleFilter);
        filterFieldMap.put(FieldFilter.DISEASE, termNameFilter);
        filterFieldMap.put(FieldFilter.ASSOCIATION_TYPE, associationFilter);
        filterFieldMap.put(FieldFilter.ORTHOLOG, orthologFilter);
        filterFieldMap.put(FieldFilter.ORTHOLOG_SPECIES, orthologSpeciesFilter);
        filterFieldMap.put(FieldFilter.EVIDENCE_CODE, evidenceCodeFilter);
        filterFieldMap.put(FieldFilter.FREFERENCE, referenceFilter);
        filterFieldMap.put(FieldFilter.SOURCE, sourceFilter);
        filterFieldMap.put(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityTypeFilter);
        filterFieldMap.put(FieldFilter.GENETIC_ENTITY, geneticEntityFilter);
        filterFieldMap.put(FieldFilter.GENE_NAME, geneNameFilter);
        filterFieldMap.put(FieldFilter.SPECIES, geneSpeciesFilter);
        filterFieldMap.put(FieldFilter.BASED_ON_GENE, basedOnGeneFilter);
        filterFieldMap.put(FieldFilter.INCLUDE_NEGATION, includeNegationFilter);
    }

}

