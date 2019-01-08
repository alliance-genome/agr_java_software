package org.alliancegenome.core.translators.document;

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
import org.alliancegenome.neo4j.entity.node.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

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

        if (entity.getDiseaseEntityJoins() == null)
            return doc;

        Map<Gene, Map<String, List<DiseaseEntityJoin>>> sortedGeneAssociationMap = getGeneAnnotationMap(entity, gene);
        List<AnnotationDocument> annotationDocuments = generateAnnotationDocument(entity, sortedGeneAssociationMap);
        doc.setAnnotations(annotationDocuments);
        return doc;
    }

    protected DiseaseDocument entityToDocument(DOTerm entity, Gene gene, List<DiseaseEntityJoin> dejList) {
        DiseaseDocument doc = getTermDiseaseDocument(entity);

        if (dejList == null)
            return doc;

        Map<String, List<DiseaseEntityJoin>> associationMap = dejList.stream()
                .collect(Collectors.groupingBy(EntityJoin::getJoinType));
        Map<Gene, Map<String, List<DiseaseEntityJoin>>> map = new HashMap<>();
        map.put(gene, associationMap);
        // create AnnotationDocument objects per
        // disease, gene, allele, association type
        List<AnnotationDocument> annotationDocuments = generateAnnotationDocument(entity, map);
        doc.setAnnotations(annotationDocuments);
        return doc;
    }

    public List<DiseaseDocument> getDiseaseDocuments(Gene gene, List<DiseaseEntityJoin> diseaseJoins, int translationDepth) {
        // group by disease
        Map<DOTerm, List<DiseaseEntityJoin>> diseaseMap = diseaseJoins.stream()
                .collect(Collectors.groupingBy(DiseaseEntityJoin::getDisease));
        List<DiseaseDocument> diseaseList = new ArrayList<>();
        // for each disease create annotation doc
        // diseaseEntityJoin list turns into AnnotationDocument objects
        diseaseMap.forEach((doTerm, diseaseEntityJoins) -> {
            try {
                DiseaseDocument doc = entityToDocument(doTerm, gene, diseaseEntityJoins); // This needs to not happen if being called from DiseaseTranslator
                if (!diseaseList.contains(doc))
                    diseaseList.add(doc);
            } catch (Exception e) {
                log.error("Exception Creating Disease Document: " + e.getMessage());
            }

        });
        return diseaseList;
    }

    private List<AnnotationDocument> generateAnnotationDocument(DOTerm entity, Map<Gene, Map<String, List<DiseaseEntityJoin>>> sortedGeneAssociationMap) {
        // generate AnnotationDocument records
        return sortedGeneAssociationMap.entrySet().stream()
                .map(geneMapEntry ->
                        geneMapEntry.getValue().entrySet().stream()
                                .map(associationEntry -> {
                                    List<DiseaseEntityJoin> alleleJoins = associationEntry.getValue().stream()
                                            .filter(join -> join.getAllele() != null)
                                            .collect(toList());
                                    List<DiseaseEntityJoin> allelelessJoins = associationEntry.getValue().stream()
                                            .filter(join -> join.getAllele() == null)
                                            .collect(toList());

                                    Map<Allele, List<DiseaseEntityJoin>> alleleMap = alleleJoins.stream()
                                            .filter(entry -> entity != null)
                                            .collect(Collectors.groupingBy(DiseaseEntityJoin::getAllele
                                            ));
                                    // add the allele-less diseaseEntityJoins under the null key into the map.
                                    if (!allelelessJoins.isEmpty())
                                        alleleMap.put(null, allelelessJoins);
                                    return alleleMap.entrySet().stream()
                                            .map(alleleMapEntry -> {
                                                // group by ortho Gene so all sources / pubs for no ortho gene are collated into a single annotation doc
                                                // each ortho gene record should be a single annotation
                                                Map<Optional<Gene>, List<DiseaseEntityJoin>> orthoMap = alleleMapEntry.getValue().stream()
                                                        .collect(groupingBy(diseaseEntityJoin -> Optional.ofNullable(diseaseEntityJoin.getOrthologyGene())));

                                                return orthoMap.entrySet().stream()
                                                        .map(orthoEntry -> {
                                                            AnnotationDocument document = new AnnotationDocument();
                                                            Gene gene = geneMapEntry.getKey();
                                                            document.setGeneDocument(geneTranslator.translate(gene, 0));
                                                            Allele allele = alleleMapEntry.getKey();
                                                            if (allele != null) {
                                                                document.setAlleleDocument(alleleTranslator.translate(allele, 0));
                                                            }
                                                            document.setAssociationType(associationEntry.getKey());
                                                            document.setSource(getSourceUrls(entity, gene.getSpecies()));
                                                            if (orthoEntry.getKey().isPresent()) {
                                                                Gene orthologyGene = orthoEntry.getKey().get();
                                                                document.setOrthologyGeneDocument(geneTranslator.translate(orthologyGene, 0));
                                                                SourceDoclet doclet = new SourceDoclet();
                                                                doclet.setName(orthoEntry.getValue().get(0).getDataProvider());
                                                                document.setSource(doclet);
                                                            }
                                                            document.setPublications(publicationDocletTranslator.getPublicationDoclets(orthoEntry.getValue()));
                                                            return document;
                                                        })
                                                        .collect(Collectors.toSet());
                                            })
                                            .flatMap(Collection::stream)
                                            .collect(Collectors.toList());
                                })
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList()))
                // turn List<AnnotationDocument> into stream<AnnotationDocument> so they can be collected into
                // the outer List<AnnotationDocument>
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private Map<Gene, Map<String, List<DiseaseEntityJoin>>> getGeneAnnotationMap(DOTerm entity, Gene gene) {
        // group by gene then by association type
        Map<Gene, Map<String, List<DiseaseEntityJoin>>> geneAssociationMap = entity.getDiseaseEntityJoins().stream()
                .filter(diseaseEntityJoin -> gene == null || diseaseEntityJoin.getGene().equals(gene))
                .collect(
                        groupingBy(DiseaseEntityJoin::getGene,
                                groupingBy(DiseaseEntityJoin::getJoinType))
                );

        // sort by gene symbol
        return geneAssociationMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
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

        // set parents
        if (doTerm.getParents() != null) {
            List<DiseaseDocument> parentDocs = doTerm.getParents().stream()
                    .map(term -> getTermDiseaseDocument(term, true))
                    .collect(Collectors.toList());
            document.setParents(parentDocs);
        }

        // set children
        if (doTerm.getChildren() != null) {
            List<DiseaseDocument> childrenDocs = doTerm.getChildren().stream()
                    .map(term -> getTermDiseaseDocument(term, true))
                    .collect(Collectors.toList());
            document.setChildren(childrenDocs);
        }

        // set highLevelSlim values
        if (CollectionUtils.isNotEmpty(doTerm.getHighLevelTermList())) {
            doTerm.getHighLevelTermList().forEach(slimTerm ->
                    document.getHighLevelSlimTermNames().add(slimTerm.getName()));
        }

        // set all parent Names
        if (CollectionUtils.isNotEmpty(doTerm.getHighLevelTermList())) {
            doTerm.getHighLevelTermList().forEach(slimTerm ->
                    document.getHighLevelSlimTermNames().add(slimTerm.getName()));
        }

        // set all sources except Human
        document.setSourceList(getSourceUrls(doTerm).stream()
                .filter(sourceDoclet -> !sourceDoclet.getSpecies().getTaxonID().equals(SpeciesType.HUMAN.getTaxonID()))
                .collect(Collectors.toList()));

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
