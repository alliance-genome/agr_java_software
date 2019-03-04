package org.alliancegenome.core.translators.document;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.core.translators.doclet.CrossReferenceDocletTranslator;
import org.alliancegenome.core.translators.doclet.PublicationDocletListTranslator;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.es.index.site.doclet.SourceDoclet;
import org.alliancegenome.es.index.site.doclet.SpeciesDoclet;
import org.alliancegenome.es.index.site.document.AnnotationDocument;
import org.alliancegenome.es.index.site.document.DiseaseAnnotationDocument;
import org.alliancegenome.es.index.site.document.DiseaseDocument;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.DiseaseEntityJoin;
import org.alliancegenome.neo4j.entity.node.EntityJoin;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.Species;
import org.alliancegenome.neo4j.entity.node.Synonym;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiseaseTranslator extends EntityDocumentTranslator<DOTerm, DiseaseDocument> {

    private static GeneTranslator geneTranslator = new GeneTranslator();
    private static AlleleTranslator alleleTranslator = new AlleleTranslator();
    private static CrossReferenceDocletTranslator crossReferenceTranslator = new CrossReferenceDocletTranslator();
    private static PublicationDocletListTranslator publicationDocletTranslator = new PublicationDocletListTranslator();

    private final Logger log = LogManager.getLogger(getClass());

    @Override
    protected DiseaseDocument entityToDocument(DOTerm entity, int translationDepth) {
        return entityToDocument(entity, null, translationDepth);
    }


    protected DiseaseDocument entityToDocument(DOTerm entity, Gene gene, int translationDepth) {
        DiseaseDocument doc = getTermDiseaseDocument(entity);

        return doc;
    }


    public Map<Gene, Map<Optional<Allele>, List<DiseaseEntityJoin>>> getGeneAlleleAnnotationMap(DOTerm entity, Gene gene) {
        // group by gene then by allele
        Map<Gene, Map<Optional<Allele>, List<DiseaseEntityJoin>>> geneAssociationMap = entity.getDiseaseEntityJoins().stream()
                .filter(diseaseEntityJoin -> gene == null || diseaseEntityJoin.getGene().equals(gene))
                .collect(
                        groupingBy(DiseaseEntityJoin::getGene,
                                groupingBy(join -> Optional.ofNullable(join.getAllele())))
                );

        // sort by gene symbol
        return geneAssociationMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private DiseaseDocument getTermDiseaseDocument(DOTerm doTerm) {
        return getTermDiseaseDocument(doTerm, false);
    }


    private DiseaseDocument getTermDiseaseDocument(DOTerm doTerm, boolean shallow) {
        DiseaseDocument document = new DiseaseDocument();
        if (doTerm.getDoId() != null)
            document.setDoId(doTerm.getDoId());
        document.setPrimaryKey(doTerm.getPrimaryKey());
        document.setPrimaryId(doTerm.getPrimaryKey());
        document.setName(doTerm.getName());
        document.setNameKey(doTerm.getName());
        document.setDefinition(doTerm.getDefinition());
        document.setDefinitionLinks(doTerm.getDefLinks());
        document.setDateProduced(doTerm.getDateProduced());
        //document.setParentDiseaseNames(getParentNameList(doTerm));
        if (doTerm.getSynonyms() != null) {
            List<String> synonymList = doTerm.getSynonyms().stream()
                    .map(Synonym::getPrimaryKey)
                    .collect(Collectors.toList());
            document.setSynonyms(synonymList);
        }
        // add CrossReferences
        if (doTerm.getCrossReferences() != null) {
            document.setCrossReferencesMap(doTerm.getCrossReferences()
                    .stream()
                    .map(crossReference -> crossReferenceTranslator.translate(crossReference))
                    .collect(Collectors.groupingBy(CrossReferenceDoclet::getType, Collectors.toList())));
        }

        if (shallow)
            return document;



        return document;
    }

    private SourceDoclet getSourceUrls(DOTerm doTerm, Species species) {
        List<SourceDoclet> sources;
        sources = getSourceUrls(doTerm).stream().
                filter(sourceUrl ->
                        sourceUrl.getSpecies().getTaxonID().equals(species.getType().getTaxonID())
                )
                .collect(Collectors.toList());
        if (sources.isEmpty())
            return null;
        return sources.get(0);
    }

    private List<SourceDoclet> getSourceUrls(DOTerm doTerm) {

        List<SourceDoclet> sourceDoclets = Arrays.stream(SpeciesType.values())
                .map(speciesType -> {
                    SourceDoclet doclet = new SourceDoclet();
                    doclet.setSpecies(speciesType.getDoclet());
                    doclet.setName(speciesType.getDisplayName());
                    if (speciesType.equals(SpeciesType.HUMAN)) {
                        doclet.setName(SpeciesType.RAT.getDisplayName());
                        doclet.setDiseaseUrl(doTerm.getHumanOnlyRgdLink());
                    }
                    if (speciesType == SpeciesType.FLY && doTerm.getFlybaseLink() != null) {
                        doclet.setUrl(doTerm.getFlybaseLink());
                        doclet.setDiseaseUrl(doTerm.getFlybaseLink());
                    }
                    if (speciesType == SpeciesType.RAT && doTerm.getRgdLink() != null) {
                        doclet.setUrl(doTerm.getRgdLink());
                        doclet.setDiseaseUrl(doTerm.getRatOnlyRgdLink());
                    }
                    if (speciesType == SpeciesType.MOUSE && doTerm.getMgiLink() != null) {
                        doclet.setUrl(doTerm.getMgiLink());
                        doclet.setDiseaseUrl(doTerm.getMgiLink());
                    }
                    if (speciesType == SpeciesType.ZEBRAFISH && doTerm.getZfinLink() != null) {
                        doclet.setUrl(doTerm.getZfinLink());
                        doclet.setDiseaseUrl(doTerm.getZfinLink());
                    }
                    if (speciesType == SpeciesType.HUMAN && doTerm.getHumanLink() != null) {
                        doclet.setUrl(doTerm.getHumanLink());
                    }
                    if (speciesType == SpeciesType.YEAST && doTerm.getSgdLink() != null) {
                        doclet.setUrl(doTerm.getSgdLink());
                        doclet.setDiseaseUrl(doTerm.getSgdLink());
                    }
                    if (speciesType == SpeciesType.WORM && doTerm.getWormbaseLink() != null) {
                        doclet.setUrl(doTerm.getWormbaseLink());
                        doclet.setDiseaseUrl(doTerm.getWormbaseLink());
                    }
                    return doclet;
                })
                .collect(toList());

        return sourceDoclets;
    }

    @Override
    protected DOTerm documentToEntity(DiseaseDocument document, int translationDepth) {
        return null;
    }

    public Iterable<DiseaseAnnotationDocument> translateAnnotationEntities(List<DOTerm> geneDiseaseList, int translationDepth) {
        Set<DiseaseAnnotationDocument> diseaseAnnotationDocuments = new HashSet<>();
        // loop over all disease terms
        geneDiseaseList.forEach(doTerm -> {
            Map<Gene, Map<Optional<Allele>, List<DiseaseEntityJoin>>> sortedGeneAssociationMap = getGeneAlleleAnnotationMap(doTerm, null);
            // loop over each gene
            sortedGeneAssociationMap.forEach((gene, alleleDiseaseMap) -> {
                // loop over each allele (may be null)
                alleleDiseaseMap.forEach((optionalAllele, associationDiseaseEntityJoinList) -> {
                    // group by association type and orthologous gene
                    Map<String, Map<Optional<Gene>, List<DiseaseEntityJoin>>> associationTypeMap = associationDiseaseEntityJoinList.stream()
                            .collect(groupingBy(DiseaseEntityJoin::getJoinType, groupingBy(diseaseEntityJoin ->
                                    Optional.ofNullable(diseaseEntityJoin.getOrthologyGene()))));
                    // loop over each association type
                    associationTypeMap.forEach((associationType, optionalGeneMap) -> {
                        optionalGeneMap.forEach((optionalOrthoGene, diseaseEntityJoinList) -> {
                            DiseaseAnnotationDocument document = new DiseaseAnnotationDocument();
                            if (translationDepth > 0) {
                                document.setGeneDocument(geneTranslator.translate(gene, translationDepth - 1)); // This needs to not happen if being call from GeneTranslator
                            }
                            String primaryKey = doTerm.getPrimaryKey() + ":" + gene.getPrimaryKey() + ":" + associationType;
                            document.setDiseaseName(doTerm.getName());
                            document.setDiseaseID(doTerm.getPrimaryKey());
                            document.setParentDiseaseIDs(getParentIdList(doTerm));
                            document.setAssociationType(associationType);
                            document.setSpecies(getSpeciesDoclet(gene.getSpecies()));
                            if (optionalOrthoGene.isPresent()) {
                                Gene orthologyGene = optionalOrthoGene.get();
                                document.setOrthologyGene(geneTranslator.translate(orthologyGene, 0));
                                SourceDoclet doclet = new SourceDoclet();
                                doclet.setName(diseaseEntityJoinList.get(0).getDataProvider());
                                document.setSource(doclet);
                                primaryKey += ":" + orthologyGene.getPrimaryKey();
                            } else {
                                document.setSource(getSourceUrls(doTerm, gene.getSpecies()));
                            }
                            document.setPublications(publicationDocletTranslator.getPublicationDoclets(diseaseEntityJoinList));
                            if (optionalAllele.isPresent()) {
                                primaryKey += ":" + optionalAllele.get().getPrimaryKey();
                                document.setAlleleDocument(alleleTranslator.translate(optionalAllele.get(), 0));
                            }
                            document.setPrimaryKey(primaryKey);
                            diseaseAnnotationDocuments.add(document);

                        });
                    });
                });
            });
        });
        return diseaseAnnotationDocuments;
    }

    private Gene getOrthologyGene(List<DiseaseEntityJoin> diseaseEntityJoinList) {
        if (diseaseEntityJoinList == null)
            return null;
        List<Gene> genes = new ArrayList<>();
        diseaseEntityJoinList.forEach(diseaseEntityJoin -> {
            if (diseaseEntityJoin.getOrthologyGene() != null)
                genes.add(diseaseEntityJoin.getOrthologyGene());
        });

        return genes.size() > 0 ? genes.get(0) : null;
    }

    /**
     * Get all the parent termIDs compiled.
     */
    private Set<String> getParentIdList(DOTerm doTerm) {
        Set<String> idList = new LinkedHashSet<>();
        idList.add(doTerm.getPrimaryKey());
        doTerm.getParents().forEach(term -> {
            idList.add(term.getPrimaryKey());
            if (term.getParents() != null)
                idList.addAll(getParentIdList(term));
        });
        return idList;
    }

    private SpeciesDoclet getSpeciesDoclet(Species species) {
        return species.getType().getDoclet();
    }
}
