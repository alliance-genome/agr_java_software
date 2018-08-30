package org.alliancegenome.neo4j.repository;

import java.util.*;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.model.Result;

public class GeneRepository extends Neo4jRepository<Gene> {

    private final Logger log = LogManager.getLogger(getClass());

    public GeneRepository() {
        super(Gene.class);
    }

    public Gene getOneGene(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";

        query += " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH p5=(g)--(s:DiseaseEntityJoin)--(feature:Feature)";
        query += " OPTIONAL MATCH p2=(do:DOTerm)--(s:DiseaseEntityJoin)-[:EVIDENCE]-(ea)";
        query += " OPTIONAL MATCH p4=(g)--(s:OrthologyGeneJoin)--(a:OrthoAlgorithm), p3=(g)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]-(q2:Species), (s)--(g2)";
        query += " OPTIONAL MATCH p6=(g)--(s:PhenotypeEntityJoin)--(tt) ";
        query += " OPTIONAL MATCH p8=(g)--(s:PhenotypeEntityJoin)--(ff:Feature)";
        query += " OPTIONAL MATCH p9=(g)--(s:GOTerm)-[:IS_A|:PART_OF*]->(parent:GOTerm)";
        query += " OPTIONAL MATCH p10=(g)--(s:BioEntityGeneExpressionJoin)--(t) ";
        query += " RETURN p1, p2, p3, p4, p5, p6, p8, p9, p10";
        //query += " RETURN p1, p2, p3, p4, p5, p6, p7, p8, p9";

        Iterable<Gene> genes = query(query, map);
        for (Gene g : genes) {
            if (g.getPrimaryKey().equals(primaryKey)) {
                return g;
            }
        }

        return null;
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

    public List<BioEntityGeneExpressionJoin> getExpressionAnnotations(List<String> geneIDs, Pagination pagination) {
        HashMap<FieldFilter, String> fieldColumnMapping = new HashMap<>();
        fieldColumnMapping.put(FieldFilter.SPECIES, "gene.species");
        fieldColumnMapping.put(FieldFilter.GENE_NAME, "gene.symbol");
        fieldColumnMapping.put(FieldFilter.SOURCE, "gene.dataProvider");
        fieldColumnMapping.put(FieldFilter.REFERENCE, "t.pubmedID");

        Map<String, String> map = new HashMap<>();
        map.put("pk", "GO:0016020");

        StringJoiner sj = new StringJoiner(",", "[", "]");
        geneIDs.forEach(geneID -> sj.add("'" + geneID + "'"));

        String query = " MATCH p1=(gene:Gene)-->(s:BioEntityGeneExpressionJoin)--(t) " +
                "WHERE gene.primaryKey in " + sj.toString();
/*
        query += " OPTIONAL MATCH slim=(o)-[:IS_A*]->(slimTerm) " +
                " where all (primaryKey IN ['GO:0016020'] where primaryKey in slimTerm.primaryKey) ";
*/

        for (FieldFilter filter : pagination.getFieldFilterValueMap().keySet()) {
            String column = fieldColumnMapping.get(filter);
            query += " AND " + column + " = '" + pagination.getFieldFilterValueMap().get(filter) + "' ";
        }
        query += " OPTIONAL MATCH p2=(t:ExpressionBioEntity)--(o:Ontology) ";
        query += " RETURN s, p1, p2 order by gene.taxonID, gene.symbol ";

        Iterable<BioEntityGeneExpressionJoin> joins = neo4jSession.query(BioEntityGeneExpressionJoin.class, query, map);
        Result result = queryForResult(query);

        List<BioEntityGeneExpressionJoin> joinList = new ArrayList<>();
        int index = 0;
        for (BioEntityGeneExpressionJoin join : joins) {
            if (!pagination.isCount()) {
                if (index >= pagination.getStart() && index < pagination.getEnd())
                    joinList.add(join);
            } else {
                joinList.add(join);
            }
            index++;
        }
        return joinList;
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

    public List<InteractionGeneJoin> getInteractions(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);

        String query = "MATCH p1=(sp1:Species)-[:FROM_SPECIES]-(g:Gene)-[iw:INTERACTS_WITH]->(g2:Gene)-[:FROM_SPECIES]-(sp2:Species), p2=(g:Gene)-->(igj:InteractionGeneJoin)--(s) where g.primaryKey = {primaryKey} and iw.uuid = igj.primaryKey RETURN p1, p2";
        //String query = "MATCH p1=(g:Gene)-[iw:INTERACTS_WITH]->(g2:Gene), p2=(g:Gene)-->(igj:InteractionGeneJoin)--(s) where g.primaryKey = {primaryKey} and iw.uuid = igj.primaryKey RETURN p1, p2";

        Iterable<Gene> genes = query(query, map);

        for (Gene g : genes) {
            if (g.getPrimaryKey().equals(primaryKey)) {
                return g.getInteractions();
            }
        }
        return null;

    }

    public Map<String, String> getGoParentList() {
        Map<String, String> slim = new HashMap<>();

        slim.put("GO:0005576", "extracellular");
        slim.put("GO:0005737", "cytoplasm");
        slim.put("GO:0005856", "cytoskeleton");
        slim.put("GO:0005739", "mitochondrion");
        slim.put("GO:0005634", "nucleus");
        slim.put("GO:0005694", "chromosome");
        slim.put("GO:0016020", "membrane");
        slim.put("GO:0031982", "vesicle");
        slim.put("GO:0071944", "cell periphery");
        slim.put("GO:0030054", "cell junction");
        slim.put("GO:0042995", "cell projection");
        slim.put("GO:0032991", "macromolecular complex");
        slim.put("GO:0045202", "synapse");
        slim.put("GO:0005575", "other locations");
        return slim;
    }

    public List<String> getGOParentTerms(ExpressionBioEntity entity) {
        List<String> parentList = new ArrayList<>();
        getGoParentList().keySet().forEach(termID -> {
            String query = " MATCH p1=(entity:ExpressionBioEntity)-[:CELLULAR_COMPONENT]-(ontology:GOTerm) ";
            query += "WHERE ontology.primaryKey = '" + entity.getGoTerm().getPrimaryKey() + "'";
            query += " OPTIONAL MATCH slim=(ontology)-[:PART_OF|IS_A*]->(slimTerm) " +
                    //" where all (primaryKey IN [" + joiner.toString() + "] where primaryKey in slimTerm.primaryKey) ";
                    " where all (primaryKey IN ['";
            query += termID + "'] where primaryKey in slimTerm.primaryKey) ";
            query += " RETURN p1, slim ";
            Iterable<GOTerm> joins = neo4jSession.query(GOTerm.class, query, new HashMap<>());
            for (GOTerm join : joins) {
                if (join.getPrimaryKey().equals(termID)) {
                    parentList.add(termID);
                }
            }
        });
        return parentList;
    }
}
