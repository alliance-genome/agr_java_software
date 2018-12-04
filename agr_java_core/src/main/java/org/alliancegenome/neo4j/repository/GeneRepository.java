package org.alliancegenome.neo4j.repository;

import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.BioEntityGeneExpressionJoin;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.Ontology;
import org.alliancegenome.neo4j.entity.node.UBERONTerm;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.model.Result;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GeneRepository extends Neo4jRepository<Gene> {

    public static final String GOSLIM_AGR = "goslim_agr";
    public static final String CELLULAR_COMPONENT = "CELLULAR_COMPONENT";
    public static final String OTHER_LOCATIONS = "other locations";
    public static final String GO_OTHER_LOCATIONS_ID = "GO:otherLocations";
    private final Logger log = LogManager.getLogger(getClass());

    public GeneRepository() {
        super(Gene.class);
    }




    /*
        query += " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene) WHERE g.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH pSyn=(g:Gene)-[:ALSO_KNOWN_AS]-(:Synonym) ";
        query += " OPTIONAL MATCH pCR=(g:Gene)-[:CROSS_REFERENCE]-(:CrossReference)";
        query += " OPTIONAL MATCH pChr=(g:Gene)-[:LOCATED_ON]-(:Chromosome)";

     */




    public Gene getOneGene(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";

        query += " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH p5=(g:Gene)--(s:DiseaseEntityJoin)--(any)";
        query += " OPTIONAL MATCH p2=(do:DOTerm)--(s:DiseaseEntityJoin)-[:EVIDENCE]-(ea)";
        query += " OPTIONAL MATCH p6=(g:Gene)--(s:PhenotypeEntityJoin)--(tt) ";
        query += " OPTIONAL MATCH featureCrossRef=(s)--(ff:Feature)--(crossRef:CrossReference) WHERE s:PhenotypeEntityJoin OR s:DiseaseEntityJoin";
        query += " OPTIONAL MATCH p9=(g:Gene)--(s:GOTerm)-[:IS_A|PART_OF*]->(:GOTerm)";
        query += " OPTIONAL MATCH p10=(g:Gene)--(s:BioEntityGeneExpressionJoin)--(t) ";
        query += " OPTIONAL MATCH pTerm=(g:Gene)--(s:ExpressionBioEntity)--(:Ontology) ";
        query += " RETURN p1, p2, p3, p5, p6, p9, p10, pTerm, featureCrossRef";

        Iterable<Gene> genes = query(query, map);
        for (Gene g : genes) {
            if (g.getPrimaryKey().equals(primaryKey)) {
                addPhenotypeListToGene(g);
                addGOListsToGene(g);
                addExpressionListsToGene(g);
                return g;
            }
        }

        return null;
    }




    private void addPhenotypeListToGene(Gene gene) {
        String query = "MATCH (gene:Gene)--(phenotype:Phenotype) WHERE gene.primaryKey={primaryKey} " +
                "RETURN phenotype.phenotypeStatement";
        gene.setPhenotypeStatements(getSetForGene(query, "phenotype.phenotypeStatement", gene.getPrimaryKey()));
    }



    private void addGOListsToGene(Gene gene) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(g:Gene)--(:GOTerm)-[:COMPUTED_CLOSURE]-(term:GOTerm) " +
                "WHERE g.primaryKey={primaryKey} " +
                "RETURN distinct LABELS(term), term.type, term.name, " +
                "'goslim_agr' IN term.subset as termInSlim ";

        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", gene.getPrimaryKey());

        Result r = queryForResult(query,map);
        Iterator<Map<String, Object>> i = r.iterator();


        while (i.hasNext()) {
            Map<String, Object> resultMap = i.next();

            String term = resultMap.get("term.name") == null ? null : resultMap.get("term.name").toString();
            String termType = resultMap.get("term.type") == null ? null : resultMap.get("term.type").toString();
            Boolean termInSlim = resultMap.get("termInSlim") == null ? false : Boolean.valueOf(resultMap.get("termInSlim").toString());

            addTermNameToGene(gene, term, termType);
            addTermToGoSlim(gene, termType, term, termInSlim);
        }
    }

    private void addTermGoSlimCache(GeneDocumentCache geneDocumentCache, String primaryKey,
                                    String termType, String term, Boolean inSlim) {
        if (StringUtils.isEmpty(term)) { return; }
        if (inSlim) {
            if (StringUtils.equals(termType, "biological_process")) {
                geneDocumentCache.getBiologicalProcessAgrSlim().computeIfAbsent(primaryKey, x -> new HashSet<>());
                geneDocumentCache.getBiologicalProcessAgrSlim().get(primaryKey).add(term);
            } else if (StringUtils.equals(termType, "cellular_component")) {
                geneDocumentCache.getCellularComponentAgrSlim().computeIfAbsent(primaryKey, x -> new HashSet<>());
                geneDocumentCache.getCellularComponentAgrSlim().get(primaryKey).add(term);
            } else if (StringUtils.equals(termType, "molecular_function")) {
                geneDocumentCache.getMolecularFunctionAgrSlim().computeIfAbsent(primaryKey, x -> new HashSet<>());
                geneDocumentCache.getMolecularFunctionAgrSlim().get(primaryKey).add(term);
            }
        }
    }

    private void addTermToGoSlim(Gene gene, String termType, String term, Boolean inSlim) {
        if (StringUtils.isEmpty(term)) { return; }
        if (inSlim) {
            if (StringUtils.equals(termType, "biological_process")) {
                gene.getBiologicalProcessAgrSlim().add(term);
            } else if (StringUtils.equals(termType, "cellular_component")) {
                gene.getCellularComponentAgrSlim().add(term);
            } else if (StringUtils.equals(termType, "molecular_function")) {
                gene.getMolecularFunctionAgrSlim().add(term);
            }
        }
    }

    private void addTermGoCache(GeneDocumentCache geneDocumentCache, String primaryKey,
                                String term, String termType) {
        if (StringUtils.isEmpty(term)) { return; }
        if (StringUtils.equals(termType, "biological_process")) {
            geneDocumentCache.getBiologicalProcessWithParents().computeIfAbsent(primaryKey, x -> new HashSet<>());
            geneDocumentCache.getBiologicalProcessWithParents().get(primaryKey).add(term);
        } else if (StringUtils.equals(termType, "cellular_component")) {
            geneDocumentCache.getCellularComponentWithParents().computeIfAbsent(primaryKey, x -> new HashSet<>());
            geneDocumentCache.getCellularComponentWithParents().get(primaryKey).add(term);
        } else if (StringUtils.equals(termType, "molecular_function")) {
            geneDocumentCache.getMolecularFunctionWithParents().computeIfAbsent(primaryKey, x -> new HashSet<>());
            geneDocumentCache.getMolecularFunctionWithParents().get(primaryKey).add(term);
        }
    }


    private void addTermNameToGene(Gene gene, String term, String termType) {
        if (StringUtils.isEmpty(term)) { return; }
        if (StringUtils.equals(termType, "biological_process")) {
            gene.getBiologicalProcessWithParents().add(term);
        } else if (StringUtils.equals(termType, "cellular_component")) {
            gene.getCellularComponentWithParents().add(term);
        } else if (StringUtils.equals(termType, "molecular_function")) {
            gene.getMolecularFunctionWithParents().add(term);
        }
    }




    public void addExpressionListsToGeneDocumentCache(GeneDocumentCache geneDocumentCache, String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(g:Gene)--(ebe:ExpressionBioEntity)-[r]-(:Ontology)-[:COMPUTED_CLOSURE]-(term:Ontology) ";
        if (StringUtils.isNotEmpty(species)) {
            query += " WHERE species.name = {species} ";
        }
        query +=  " RETURN distinct g.primaryKey, TYPE(r), ebe.whereExpressedStatement, term.name";

        Map<String,String> params = new HashMap<>();
        if (StringUtils.isNotEmpty(species)) {
            params.put("species", species);
        }
        Result r = queryForResult(query, params);

        Iterator<Map<String, Object>> i = r.iterator();


        while (i.hasNext()) {
            Map<String, Object> resultMap = i.next();

            String primaryKey = (String) resultMap.get("g.primaryKey");
            String relationshipType = resultMap.get("TYPE(r)") == null ? null : resultMap.get("TYPE(r)").toString();
            String term = resultMap.get("term.name") == null ? null : resultMap.get("term.name").toString();

            String whereExpressed = resultMap.get("ebe.whereExpressedStatement") == null ? null : resultMap.get("ebe.whereExpressedStatement").toString();

            geneDocumentCache.getWhereExpressed().computeIfAbsent(primaryKey, x -> new HashSet<>());
            geneDocumentCache.getWhereExpressed().get(primaryKey).add(whereExpressed);

            if (StringUtils.equals(relationshipType,"CELLULAR_COMPONENT_RIBBON_TERM") && StringUtils.isNotEmpty(term)) {
                geneDocumentCache.getCellularComponentExpressionAgrSlim().computeIfAbsent(primaryKey, x -> new HashSet<>());
                geneDocumentCache.getCellularComponentExpressionAgrSlim().get(primaryKey).add(term);
            } else if (StringUtils.equals(relationshipType, "ANATOMICAL_RIBBON_TERM") && StringUtils.isNotEmpty(term)) {
                geneDocumentCache.getAnatomicalExpression().computeIfAbsent(primaryKey, x -> new HashSet<>());
                geneDocumentCache.getAnatomicalExpression().get(primaryKey).add(term);
            } else if (StringUtils.equals(relationshipType,"CELLULAR_COMPONENT") && StringUtils.isNotEmpty(term)) {
                geneDocumentCache.getCellularComponentWithParents().computeIfAbsent(primaryKey, x -> new HashSet<>());
                geneDocumentCache.getCellularComponentWithParents().get(primaryKey).add(term);
            } else if (StringUtils.equals(relationshipType,"ANATOMICAL_STRUCTURE") && StringUtils.isNotEmpty(term)) {
                geneDocumentCache.getAnatomicalExpressionWithParents().computeIfAbsent(primaryKey, x -> new HashSet<>());
                geneDocumentCache.getAnatomicalExpressionWithParents().get(primaryKey).add(term);
            }
        }
    }


    private void addExpressionListsToGene(Gene gene) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(g:Gene)--(ebe:ExpressionBioEntity)-[r]-(:Ontology)-[:COMPUTED_CLOSURE]-(term:Ontology)" +
                "WHERE g.primaryKey={primaryKey} " +
                "RETURN distinct TYPE(r), ebe.whereExpressedStatement, term.name";


        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", gene.getPrimaryKey());

        Result r = queryForResult(query,map);
        Iterator<Map<String, Object>> i = r.iterator();


        while (i.hasNext()) {
            Map<String, Object> resultMap = i.next();

            String relationshipType = resultMap.get("TYPE(r)") == null ? null : resultMap.get("TYPE(r)").toString();
            String term = resultMap.get("term.name") == null ? null : resultMap.get("term.name").toString();
            String whereExpressed = resultMap.get("ebe.whereExpressedStatement") == null ? null : resultMap.get("ebe.whereExpressedStatement").toString();
            gene.getWhereExpressed().add(whereExpressed);

            if (StringUtils.equals(relationshipType,"CELLULAR_COMPONENT_RIBBON_TERM") && StringUtils.isNotEmpty(term)) {
                gene.getCellularComponentExpressionAgrSlim().add(term);
            } else if (StringUtils.equals(relationshipType, "ANATOMICAL_RIBBON_TERM") && StringUtils.isNotEmpty(term)) {
                gene.getAnatomicalExpression().add(term);
            } else if (StringUtils.equals(relationshipType,"CELLULAR_COMPONENT") && StringUtils.isNotEmpty(term)) {
                gene.getCellularComponentExpressionWithParents().add(term);
            } else if (StringUtils.equals(relationshipType,"ANATOMICAL_STRUCTURE") && StringUtils.isNotEmpty(term)) {
                gene.getAnatomicalExpressionWithParents().add(term);
            }
        }
    }



    private Set<String> getSetForGene(String query, String returnField, String primaryKey) {

        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);

        Result r = queryForResult(query,map);
        Iterator<Map<String, Object>> i = r.iterator();

        Set<String> values = new HashSet<>();

        while (i.hasNext()) {
            Map<String, Object> resultMap = i.next();
            values.add((String) resultMap.get(returnField));
        }
        return values;

    }



    public Gene getExpressionGene(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";

        query += " MATCH p1=(g:Gene)-->(s:BioEntityGeneExpressionJoin)--(t) WHERE g.primaryKey = {primaryKey}";
        query += " RETURN p1";

        Iterable<Gene> genes = query(query, map);
        for (Gene g : genes) {
            if (g.getPrimaryKey().equals(primaryKey)) {
                return g;
            }
        }

        return null;
    }

    //convenience method for getting expression for a single gene
    public List<BioEntityGeneExpressionJoin> getExpressionAnnotations(Gene gene) {
        List<String> geneIDs = new ArrayList<>();
        geneIDs.add(gene.getPrimaryKey());
        return getExpressionAnnotations(geneIDs, null, new Pagination());
    }

    public List<BioEntityGeneExpressionJoin> getExpressionAnnotations(List<String> geneIDs, String termID, Pagination pagination) {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        geneIDs.forEach(geneID -> sj.add("'" + geneID + "'"));
        String sourceFilterClause = addWhereClauseString("crossRef.displayName", FieldFilter.FSOURCE, pagination);

        String query = " MATCH p1=(species:Species)--(gene:Gene)-->(s:BioEntityGeneExpressionJoin)--(t)," +
                " entity = (s:BioEntityGeneExpressionJoin)--(exp:ExpressionBioEntity)--(o:Ontology) ";
        if (sourceFilterClause != null)
            query += ", crossReference = (s:BioEntityGeneExpressionJoin)--(crossRef:CrossReference) ";
        query += "WHERE gene.primaryKey in " + sj.toString();

        String geneFilterClause = addWhereClauseString("gene.symbol", FieldFilter.GENE_NAME, pagination);
        if (geneFilterClause != null) {
            query += " AND " + geneFilterClause;
        }
        String speciesFilterClause = addWhereClauseString("species.name", FieldFilter.FSPECIES, pagination);
        if (speciesFilterClause != null) {
            query += " AND " + speciesFilterClause;
        }
        if (sourceFilterClause != null) {
            query += " AND " + sourceFilterClause;
        }
        String termFilterClause = addWhereClauseString("exp.whereExpressedStatement", FieldFilter.TERM_NAME, pagination);
        if (termFilterClause != null) {
            query += " AND " + termFilterClause;
        }
        if (sourceFilterClause == null) {
            query += " OPTIONAL MATCH crossReference = (s:BioEntityGeneExpressionJoin)--(crossRef:CrossReference) ";
        }
        query += " RETURN s, p1, crossReference, entity ";
        Iterable<BioEntityGeneExpressionJoin> joins = neo4jSession.query(BioEntityGeneExpressionJoin.class, query, new HashMap<>());


        List<BioEntityGeneExpressionJoin> joinList = new ArrayList<>();
        for (BioEntityGeneExpressionJoin join : joins) {
            // the setter of gene.species is not called in neo4j...
            // Thus, setting it manually
            join.getGene().setSpeciesName(join.getGene().getSpecies().getName());
            join.getPublication().setPubIdFromId();
            joinList.add(join);
        }

        // filtering
        joinList = joinList.stream()
                .filter(join -> passFilter(join, pagination.getFieldFilterValueMap()))
                .collect(Collectors.toList());


        // check for rollup term existence
        // Check for GO terms
        if (termID != null && !termID.isEmpty()) {
            // At the moment we are expecting only a single termID
            // GO term check
            if (Ontology.isGOTerm(termID))
                joinList = joinList.stream()
                        .filter(join -> join.getEntity().getCcRibbonTermList().stream().map(GOTerm::getPrimaryKey).anyMatch(s -> s.equals(termID)))
                        .collect(Collectors.toList());
            // AO / stage term check
            if (Ontology.isAoOrStageTerm(termID)) {
                // check AO ribbon term list
                List<BioEntityGeneExpressionJoin> aoJoinList = joinList.stream()
                        .filter(join ->
                                join.getEntity().getAoTermList().stream().map(UBERONTerm::getPrimaryKey).anyMatch(s -> s.equals(termID))
                        )
                        .collect(Collectors.toList());
                // check stage term list
                Map map = getFullAoList();
                if (aoJoinList.size() == 0) {
                    joinList = joinList.stream()
                            .filter(join -> join.getStageTerm() != null && join.getStageTerm().getPrimaryKey().equals(termID))
                            .collect(Collectors.toList());
                } else {
                    joinList = aoJoinList;
                }
            }
        }
        return joinList;
    }

    private String addWhereClauseString(String fieldName, FieldFilter fieldFilter, Pagination pagination) {
        String value = pagination.getFieldFilterValueMap().get(fieldFilter);
        String query = null;
        if (value != null) {
            query = " LOWER(" + fieldName + ") =~ '.*" + value.toLowerCase() + ".*' ";
        }
        return query;
    }

    public List<BioEntityGeneExpressionJoin> getExpressionAnnotationsByTaxon(String taxonID, String
            termID, Pagination pagination) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("taxon", taxonID);
        String query = " MATCH p1=(species:Species)--(gene:Gene)-->(s:BioEntityGeneExpressionJoin)--(t) " +
                "WHERE gene.taxonId = {taxon} ";
        query += " OPTIONAL MATCH p2=(t:ExpressionBioEntity)-->(o:Ontology) ";
        query += " RETURN s, p1, p2 ";
        Iterable<BioEntityGeneExpressionJoin> joins = neo4jSession.query(BioEntityGeneExpressionJoin.class, query, parameters);


        List<BioEntityGeneExpressionJoin> joinList = new ArrayList<>();
        for (BioEntityGeneExpressionJoin join : joins) {
            // the setter of gene.species is not called in neo4j...
            // Thus, setting it manually
            join.getGene().setSpeciesName(join.getGene().getSpecies().getName());
            join.getPublication().setPubIdFromId();
            joinList.add(join);
        }
        return joinList;
    }

    private boolean passFilter(BioEntityGeneExpressionJoin
                                       bioEntityGeneExpressionJoin, Map<FieldFilter, String> fieldFilterValueMap) {
        Map<FieldFilter, FilterComparator<BioEntityGeneExpressionJoin, String>> map = new HashMap<>();
        map.put(FieldFilter.FSPECIES, (join, filterValue) -> join.getGene().getSpeciesName().toLowerCase().contains(filterValue.toLowerCase()));
        map.put(FieldFilter.GENE_NAME, (join, filterValue) -> join.getGene().getSymbol().toLowerCase().contains(filterValue.toLowerCase()));
        map.put(FieldFilter.TERM_NAME, (join, filterValue) -> join.getEntity().getWhereExpressedStatement().toLowerCase().contains(filterValue.toLowerCase()));
        map.put(FieldFilter.STAGE, (join, filterValue) -> join.getStage().getPrimaryKey().toLowerCase().contains(filterValue.toLowerCase()));
        map.put(FieldFilter.ASSAY, (join, filterValue) -> join.getAssay().getDisplay_synonym().toLowerCase().contains(filterValue.toLowerCase()));
        map.put(FieldFilter.FREFERENCE, (join, filterValue) -> join.getPublication().getPubId().toLowerCase().contains(filterValue.toLowerCase()));
        map.put(FieldFilter.FSOURCE, (join, filterValue) -> join.getCrossReference().getDisplayName().toLowerCase().contains(filterValue.toLowerCase()));

        if (fieldFilterValueMap == null || fieldFilterValueMap.size() == 0)
            return true;
        for (FieldFilter filter : fieldFilterValueMap.keySet()) {
            if (!map.get(filter).compare(bioEntityGeneExpressionJoin, fieldFilterValueMap.get(filter)))
                return false;
        }
        return true;
    }

    public List<BioEntityGeneExpressionJoin> getExpressionAnnotationSummary(String geneID) {
        String query = " MATCH p1=(gene:Gene)-->(s:BioEntityGeneExpressionJoin)--(t) ";
        query += "WHERE gene.primaryKey = '" + geneID + "'";
        query += " OPTIONAL MATCH p2=(t:ExpressionBioEntity)--(o:Ontology) ";
        query += " RETURN s, p1, p2 order by gene.taxonID, gene.symbol ";

        Iterable<BioEntityGeneExpressionJoin> joins = neo4jSession.query(BioEntityGeneExpressionJoin.class, query, new HashMap<>());

        List<BioEntityGeneExpressionJoin> joinList = new ArrayList<>();
        for (BioEntityGeneExpressionJoin join : joins) {
            joinList.add(join);
        }
        return joinList;
    }

    public Gene getOrthologyGene(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";

        query += " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH p4=(g)--(s:OrthologyGeneJoin)--(a:OrthoAlgorithm), p3=(g)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]-(q2:Species), (s)--(g2)";
        query += " RETURN p1, p3, p4";

        Iterable<Gene> genes = query(query, map);
        for (Gene g : genes) {
            if (g.getPrimaryKey().equals(primaryKey)) {
                return g;
            }
        }
        return null;
    }

    public List<Gene> getOrthologyGenes(List<String> geneIDs) {
        HashMap<String, String> map = new HashMap<>();

        StringJoiner geneJoiner = new StringJoiner(",", "[", "]");
        geneIDs.forEach(geneID -> geneJoiner.add("'" + geneID + "'"));

        String query = " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey in " + geneJoiner;
        query += " OPTIONAL MATCH p4=(g)--(s:OrthologyGeneJoin)--(a:OrthoAlgorithm), p3=(g)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]-(q2:Species), (s)--(g2)";
        query += " RETURN p1, p3, p4";

        Iterable<Gene> genes = query(query, map);
        List<Gene> geneList = StreamSupport.stream(genes.spliterator(), false)
                .filter(gene -> geneIDs.contains(gene.getPrimaryKey()))
                .collect(Collectors.toList());

        return geneList;
    }

    public Set<Gene> getOrthologyByTwoSpecies(String speciesOne, String speciesTwo) {

        speciesOne = SpeciesType.getTaxonId(speciesOne);
        speciesTwo = SpeciesType.getTaxonId(speciesTwo);
        HashMap<String, String> map = new HashMap<>();
        map.put("speciesID", speciesOne);
        map.put("homologSpeciesID", speciesTwo);
        map.put("strict", "true");
        String query = "";

        query += " MATCH p1=(g:Gene)-[ortho:ORTHOLOGOUS]-(gh:Gene), ";
        query += "p4=(g)--(s:OrthologyGeneJoin)--(gh:Gene), " +
                "p2=(g)-[:FROM_SPECIES]-(gs:Species), " +
                "p3=(gh)-[:FROM_SPECIES]-(ghs:Species), " +
                "p5=(s)--(algorithm:OrthoAlgorithm) ";
        query += " where g.taxonId = {speciesID} and   gh.taxonId = {homologSpeciesID} and ortho.strictFilter = {strict} ";
        //query += "return g, ortho, gh, s, algorithm";
        query += "return g";

        Iterable<Gene> genes = query(query, map);
        Set<Gene> geneListp = new HashSet<>();
        genes.forEach(gene -> geneListp.add(gene));
        Set<Gene> geneList = new HashSet<>();
        for (Gene g : genes) {
            if (g.getTaxonId().equals(speciesOne) && (g.getOrthoGenes() != null && g.getOrthoGenes().size() > 0)) {
                if (log.isDebugEnabled())
                    g.getOrthoGenes().forEach(orthologous -> {
                        log.debug(orthologous.getGene1().getPrimaryKey() + " " + orthologous.getGene2().getPrimaryKey());
                    });
                geneList.add(g);
            }
        }
        return geneList;
    }

    public Set<Gene> getOrthologyBySingleSpecies(String speciesOne) {

        speciesOne = SpeciesType.getTaxonId(speciesOne);
        HashMap<String, String> map = new HashMap<>();
        map.put("speciesID", speciesOne);
        String query = "";

        query += "MATCH p1=(g:Gene)-[ortho:ORTHOLOGOUS]-(gh:Gene), ";
        query += "p2=(g)--(s:OrthologyGeneJoin)--(gh:Gene), " +
                "p3=(g)-[:FROM_SPECIES]-(gs:Species), " +
                "p4=(gh)-[:FROM_SPECIES]-(ghs:Species), " +
                "p5=(s)--(algorithm:OrthoAlgorithm) ";
        query += " where g.taxonId = {speciesID} ";
        query += "return p1, p2, p3, p4, p5";

        Iterable<Gene> genes = query(query, map);
        Set<Gene> geneList = new HashSet<>();
        for (Gene g : genes) {
            if (g.getSpecies().getPrimaryKey().equals(speciesOne)) {
                geneList.add(g);
            }
        }
        return geneList;
    }

    public HashMap<String, Gene> getGene(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";

        query += " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH p5=(g)--(s:DiseaseEntityJoin)--(feature:Feature)";
        query += " OPTIONAL MATCH p2=(do:DOTerm)--(s:DiseaseEntityJoin)-[:EVIDENCE]-(ea)";
        query += " OPTIONAL MATCH p4=(g)--(s:OrthologyGeneJoin)--(a:OrthoAlgorithm), p3=(g)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]-(q2:Species), (s)--(g2)";
        query += " RETURN p1, p2, p3, p4, p5";

        HashMap<String, Gene> retMap = new HashMap<>();

        Iterable<Gene> genes = query(query, map);
        for (Gene g : genes) {
            retMap.put(g.getPrimaryKey(), g);
        }
        return retMap;
    }

    public List<String> getAllGeneKeys() {
        String query = "MATCH (g:Gene)-[:FROM_SPECIES]-(q:Species) RETURN distinct g.primaryKey";
        Result r = queryForResult(query);
        Iterator<Map<String, Object>> i = r.iterator();
        ArrayList<String> list = new ArrayList<>();

        while (i.hasNext()) {
            Map<String, Object> map2 = i.next();
            list.add((String) map2.get("g.primaryKey"));
        }
        return list;
    }

    public List<String> getAllGeneKeys(String species) {
        String query = "MATCH (g:Gene)-[:FROM_SPECIES]-(species:Species) WHERE species.name = {species} RETURN distinct g.primaryKey";

        HashMap<String, String> params = new HashMap<>();
        params.put("species", species);

        Result r = queryForResult(query, params);
        Iterator<Map<String, Object>> i = r.iterator();
        ArrayList<String> list = new ArrayList<>();

        while (i.hasNext()) {
            Map<String, Object> map2 = i.next();
            list.add((String) map2.get("g.primaryKey"));
        }
        return list;
    }

    private LinkedHashMap<String, String> goCcList;

    public Map<String, String> getGoSlimList(String goType) {
        // cache the complete GO CC list.
        if (goCcList != null)
            return goCcList;
        String cypher = "MATCH (goTerm:GOTerm) " +
                "where all (subset IN ['" + GOSLIM_AGR + "'] where subset in goTerm.subset)  RETURN goTerm ";

        Iterable<GOTerm> joins = neo4jSession.query(GOTerm.class, cypher, new HashMap<>());

        // used for sorting the GO terms according to the order in the java script file.
        // feels pretty hacky to me but the obo file does not contain sorting info...
        List<String> goTermOrderedList = getGoTermListFromJavaScriptFile();
        goCcList = StreamSupport.stream(joins.spliterator(), false)
                .filter(goTerm -> goTerm.getType().equals(goType))
                .sorted((o1, o2) -> goTermOrderedList.indexOf(o1.getPrimaryKey()) < goTermOrderedList.indexOf(o2.getPrimaryKey()) ? -1 : 1)
                .collect(Collectors.toMap(GOTerm::getPrimaryKey, GOTerm::getName, (s, s2) -> s, LinkedHashMap::new));
        return goCcList;
    }

    // cache variable
    private List<String> goTermOrderedList;

    private List<String> getGoTermListFromJavaScriptFile() {
        if (goTermOrderedList != null)
            return goTermOrderedList;

        String url = "https://raw.githubusercontent.com/geneontology/ribbon/master/src/data/agr.js";
        List<String> content = new ArrayList<>();
        URL oracle;
        try {
            oracle = new URL(url);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(oracle.openStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null)
                content.add(inputLine);
            in.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        String pattern = "(.*)(GO:[0-9]*)(.*)";
        Pattern p = Pattern.compile(pattern);
        goTermOrderedList = content.stream()
                .filter(line -> {
                    Matcher m = p.matcher(line);
                    return m.matches();
                })
                .map(line -> {
                    Matcher m = p.matcher(line);
                    m.matches();
                    return m.group(2);
                })
                .collect(Collectors.toList());
        return goTermOrderedList;
    }

    public Map<String, String> getGoCCSlimList() {
        Map<String, String> goSlimList = getGoSlimList(CELLULAR_COMPONENT.toLowerCase());
        goSlimList.put(GO_OTHER_LOCATIONS_ID, OTHER_LOCATIONS);
        return goSlimList;
    }

    public Map<String, String> getGoCCSlimListWithoutOther() {
        return getGoSlimList(CELLULAR_COMPONENT.toLowerCase());
    }

    public List<Gene> getGenes(OrthologyFilter filter) {

        String query = getAllGenesQuery(filter);
        query += " RETURN gene order by gene.taxonID, gene.symbol ";
        query += " SKIP " + (filter.getStart() - 1) + " limit " + filter.getRows();

        Iterable<Gene> genes = query(query);
        return StreamSupport.stream(genes.spliterator(), false)
                .map(gene -> {
                    gene.setSpeciesName(SpeciesType.fromTaxonId(gene.getTaxonId()).getName());
                    return gene;
                })
                .collect(Collectors.toList());
    }

    public int getGeneCount(OrthologyFilter filter) {

        String query = getAllGenesQuery(filter);
        query += " RETURN count(gene) ";
        long count = queryCount(query);
        return (int) count;
    }

    private String getAllGenesQuery(OrthologyFilter filter) {
        StringJoiner taxonJoiner = new StringJoiner(",", "[", "]");
        String taxonClause = null;
        if (filter.getTaxonIDs() != null) {
            filter.getTaxonIDs().forEach(taxonID -> taxonJoiner.add("'" + taxonID + "'"));
            taxonClause = taxonJoiner.toString();
        }
        String query = " MATCH (gene:Gene) ";
        if (taxonClause != null) {
            query += "WHERE gene.taxonId in " + taxonClause;
        }
        return query;
    }

    public List<String> getGeneIDs(OrthologyFilter filter) {
        String query = getAllGenesQuery(filter);
        query += " RETURN gene order by gene.taxonID, gene.symbol ";
        query += " SKIP " + (filter.getStart() - 1) + " limit " + filter.getRows();

        Iterable<Gene> genes = query(query);
        return StreamSupport.stream(genes.spliterator(), false)
                .map(gene -> gene.getPrimaryKey())
                .collect(Collectors.toList());
    }

    // stage categories
    // cached
    Map<String, String> stageMap;

    static List<String> stageOrder = new ArrayList<>();

    static {
        stageOrder.add("embryo stage");
        stageOrder.add("post embryonic, pre-adult");
        stageOrder.add("post-juvenile adult stage");
    }

    public Map<String, String> getStageList() {
        if (stageMap != null)
            return stageMap;

        String cypher = "match p=(uber:UBERONTerm)-[:STAGE_RIBBON_TERM]-(:BioEntityGeneExpressionJoin) return distinct uber";

        Iterable<UBERONTerm> terms = neo4jSession.query(UBERONTerm.class, cypher, new HashMap<>());
        if (!StreamSupport.stream(terms.spliterator(), false).allMatch(uberonTerm ->
                stageOrder.indexOf(uberonTerm.getName()) > -1)) {
            String expectedValues = stageOrder.stream().collect(joining(", "));
            throw new RuntimeException("One or more stage name in UBERON has changed: \nFound values: " +
                    StreamSupport.stream(terms.spliterator(), false)
                            .map(UBERONTerm::getName)
                            .collect(joining(", ")) + " Expected Values: " + expectedValues);
        }

        stageMap = StreamSupport.stream(terms.spliterator(), false)
                .sorted(Comparator.comparingInt(o ->
                        stageOrder.indexOf(o.getName())))
                .collect(Collectors.toMap(UBERONTerm::getPrimaryKey, UBERONTerm::getName, (e1, e2) -> e2, LinkedHashMap::new));

        return stageMap;
    }

    public Map<String, String> getFullAoList() {
        String cypher = "match p=(uber:UBERONTerm)-[:ANATOMICAL_RIBBON_TERM]-(:ExpressionBioEntity) return distinct uber";

        Iterable<UBERONTerm> terms = neo4jSession.query(UBERONTerm.class, cypher, new HashMap<>());
        String alwaysLast = "other";
        Map<String, String> map = StreamSupport.stream(terms.spliterator(), false)
                .sorted((o1, o2) -> {
                    if (o1.getName().equalsIgnoreCase(alwaysLast)) {
                        return 1;
                    }
                    if (o2.getName().equalsIgnoreCase(alwaysLast)) {
                        return -1;
                    }
                    return o1.getName().compareTo(o2.getName());
                })
                .collect(Collectors.toMap(UBERONTerm::getPrimaryKey, UBERONTerm::getName, (x, y) -> x + ", " + y, LinkedHashMap::new));
        return map;
    }

    public Map<String, String> getFullGoList() {
        String cypher = "match p=(uber:GOTerm)-[:CELLULAR_COMPONENT_RIBBON_TERM]-(:ExpressionBioEntity) return distinct uber";
        Iterable<GOTerm> terms = neo4jSession.query(GOTerm.class, cypher, new HashMap<>());
        String alwaysLast = "other locations";
        return StreamSupport.stream(terms.spliterator(), false)
                .sorted((o1, o2) -> {
                    if (o1.getName().equalsIgnoreCase(alwaysLast)) {
                        return 1;
                    }
                    if (o2.getName().equalsIgnoreCase(alwaysLast)) {
                        return -1;
                    }
                    return o1.getName().compareToIgnoreCase(o2.getName());
                })
                .collect(Collectors.toMap(GOTerm::getPrimaryKey, GOTerm::getName, (x, y) -> x + ", " + y, LinkedHashMap::new));
    }

    @FunctionalInterface
    public interface FilterComparator<T, U> {
        boolean compare(T o, U oo);

        default FilterComparator<T, U> thenCompare(FilterComparator<T, U> other) {
            Objects.requireNonNull(other);
            return (FilterComparator<T, U> & Serializable) (c1, c2) -> {
                boolean res = compare(c1, c2);
                return (!res) ? res : other.compare(c1, c2);
            };
        }
    }

    class GoHighLevelTerms {
        @JsonProperty("class_id")
        private String id;
        @JsonProperty("class_label")
        private String label;
        private String separator;
    }
}
