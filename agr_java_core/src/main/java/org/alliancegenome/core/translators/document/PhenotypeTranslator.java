package org.alliancegenome.core.translators.document;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.core.translators.doclet.PublicationDocletListTranslator;
import org.alliancegenome.es.index.site.document.PhenotypeAnnotationDocument;
import org.alliancegenome.es.index.site.document.PhenotypeDocument;
import org.alliancegenome.neo4j.entity.node.Feature;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.Phenotype;
import org.alliancegenome.neo4j.entity.node.PhenotypeEntityJoin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class PhenotypeTranslator extends EntityDocumentTranslator<Phenotype, PhenotypeDocument> {

    private static GeneTranslator geneTranslator = new GeneTranslator();
    private static FeatureTranslator featureTranslator = new FeatureTranslator();
    private static PublicationDocletListTranslator publicationDocletTranslator = new PublicationDocletListTranslator();

    private final Logger log = LogManager.getLogger(getClass());

    @Override
    protected PhenotypeDocument entityToDocument(Phenotype phenotype, int translationDepth) {
        PhenotypeDocument doc = getPhenotypeDocument(phenotype);

        if (phenotype.getPhenotypeEntityJoins() == null)
            return doc;

        Map<Gene, List<PhenotypeEntityJoin>> sortedGeneMap = getGeneAnnotationMap(phenotype, null);
        List<PhenotypeAnnotationDocument> annotationDocuments = generateAnnotationDocument(phenotype, sortedGeneMap);
        doc.setAnnotations(annotationDocuments);
        return doc;
    }

    // list of phenotypeEntityJoins are of type termName and gene
    private PhenotypeDocument entityToDocument(Phenotype phenotype, Gene gene, List<PhenotypeEntityJoin> dejList) {
        PhenotypeDocument doc = getPhenotypeDocument(phenotype);

        if (dejList == null)
            return doc;

        Map<Gene, List<PhenotypeEntityJoin>> map = new HashMap<>();
        map.put(gene, dejList);
        // create AnnotationDocument objects per
        // disease, gene, Feature, association type
        List<PhenotypeAnnotationDocument> annotationDocuments = generateAnnotationDocument(phenotype, map);
        doc.setAnnotations(annotationDocuments);
        return doc;
    }

    List<PhenotypeDocument> getPhenotypeDocuments(Gene gene, List<PhenotypeEntityJoin> phenotypeJoins, int translationDepth) {
        // group by termName

        Map<Phenotype, List<PhenotypeEntityJoin>> phenotypeMap = phenotypeJoins.stream()
                .filter(join -> join.getPhenotype() != null)
                .collect(Collectors.groupingBy(PhenotypeEntityJoin::getPhenotype));
        List<PhenotypeDocument> phenotypeList = new ArrayList<>();
        // for each termName create annotation doc
        // phenotypeEntityJoin list turns into AnnotationDocument objects
        phenotypeMap.forEach((phenotype, phenotypeEntityJoins) -> {
            if (translationDepth > 0) {
                try {
                    PhenotypeDocument doc = entityToDocument(phenotype, gene, phenotypeEntityJoins); // This needs to not happen if being called from DiseaseTranslator
                    if (!phenotypeList.contains(doc))
                        phenotypeList.add(doc);
                } catch (Exception e) {
                    log.error("Exception Creating Phenotype Document: " + e.getMessage());
                }
            }

        });
        return phenotypeList;
    }

    // generate PhenotypeAnnotationDocument records
    private List<PhenotypeAnnotationDocument> generateAnnotationDocument(Phenotype phenotype, Map<Gene, List<PhenotypeEntityJoin>> sortedGeneMap) {
        assert phenotype != null;
        return sortedGeneMap.entrySet().stream()
                .map(geneMapEntry -> {
                    List<PhenotypeEntityJoin> featureJoins = geneMapEntry.getValue().stream()
                            .filter(join -> join.getFeature() != null)
                            .collect(toList());
                    List<PhenotypeEntityJoin> featurelessJoins = geneMapEntry.getValue().stream()
                            .filter(join -> join.getFeature() == null)
                            .collect(toList());

                    Map<Feature, List<PhenotypeEntityJoin>> featureMap = featureJoins.stream()
                            .collect(Collectors.groupingBy(PhenotypeEntityJoin::getFeature
                            ));
                    if (!featurelessJoins.isEmpty())
                        featureMap.put(null, featurelessJoins);
                    return featureMap.entrySet().stream()
                            .map(featureMapEntry -> {
                                PhenotypeAnnotationDocument document = new PhenotypeAnnotationDocument();
                                document.setPhenotype(phenotype.getPhenotypeStatement());
                                document.setGeneDocument(geneTranslator.translate(geneMapEntry.getKey(), 0));
                                Feature feature = featureMapEntry.getKey();
                                if (feature != null) {
                                    document.setFeatureDocument(featureTranslator.translate(feature, 0));
                                }
                                document.setPublications(publicationDocletTranslator.getPublicationDoclets(featureMapEntry.getValue()));
                                return document;
                            }).collect(Collectors.toList());
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private Map<Gene, List<PhenotypeEntityJoin>> getGeneAnnotationMap(Phenotype phenotype, Gene gene) {
        // group by gene then by association type
        Map<Gene, List<PhenotypeEntityJoin>> geneMap = phenotype.getPhenotypeEntityJoins().stream()
                .filter(phenotypeEntityJoin -> gene == null || phenotypeEntityJoin.getGene().equals(gene))
                .collect(groupingBy(PhenotypeEntityJoin::getGene));

        // sort by gene symbol
        return geneMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<Gene, Map<Optional<Feature>, List<PhenotypeEntityJoin>>> getGeneFeatureAnnotationMap(Phenotype phenotype, Gene gene) {
        // group by gene then by feature
        Map<Gene, Map<Optional<Feature>, List<PhenotypeEntityJoin>>> geneMap = phenotype.getPhenotypeEntityJoins().stream()
                .filter(phenotypeEntityJoin -> gene == null || phenotypeEntityJoin.getGene().equals(gene))
                .collect(
                        groupingBy(PhenotypeEntityJoin::getGene,
                                groupingBy(join -> Optional.ofNullable(join.getFeature())))
                );

        // sort by gene symbol
        return geneMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private PhenotypeDocument getPhenotypeDocument(Phenotype phenotype) {
        return getPhenotypeDocument(phenotype, false);
    }


    private PhenotypeDocument getPhenotypeDocument(Phenotype phenotype, boolean shallow) {
        PhenotypeDocument document = new PhenotypeDocument();
        document.setPrimaryKey(phenotype.getPrimaryKey());
        document.setPrimaryId(phenotype.getPrimaryKey());
        document.setPhenotypeStatement(phenotype.getPhenotypeStatement());
        if (shallow)
            return document;

        return document;
    }

    @Override
    protected Phenotype documentToEntity(PhenotypeDocument document, int translationDepth) {
        return null;
    }

    public Iterable<PhenotypeAnnotationDocument> translateAnnotationEntities(List<Phenotype> phenotypeList) {
        Set<PhenotypeAnnotationDocument> phenotypeAnnotationDocuments = new HashSet<>();
        // loop over all phenotypes
        phenotypeList.forEach(phenotype -> {
            Map<Gene, Map<Optional<Feature>, List<PhenotypeEntityJoin>>> sortedGeneMap = getGeneFeatureAnnotationMap(phenotype, null);
            // loop over each gene
            sortedGeneMap.forEach((gene, featurePhenotypeMap) -> {
                // loop over each feature (may be null)
                featurePhenotypeMap.forEach((optionalFeature, phenotypeEntityJoins) -> {
                    PhenotypeAnnotationDocument document = new PhenotypeAnnotationDocument();
                    document.setPhenotype(phenotype.getPhenotypeStatement());
                    document.setGeneDocument(geneTranslator.translate(gene, 0));
                    String primaryKey = phenotype.getPrimaryKey() + ":" + gene.getPrimaryKey();
                    if (optionalFeature.isPresent()) {
                        primaryKey += ":" + optionalFeature.get().getPrimaryKey();
                        document.setFeatureDocument(featureTranslator.translate(optionalFeature.get(), 0));
                    }
                    document.setPrimaryKey(primaryKey);
                    document.setPublications(publicationDocletTranslator.getPublicationDoclets(phenotypeEntityJoins));
                    phenotypeAnnotationDocuments.add(document);
                });
            });
        });
        return phenotypeAnnotationDocuments;
    }
}
