package org.alliancegenome.neo4j.repository;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Chromosome;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.neo4j.ogm.model.Result;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;

@Log4j2
public class AlleleRepository extends Neo4jRepository<Allele> {

    // for debugging
    public boolean debug;
    public List<String> testGeneIDs;

    public AlleleRepository(boolean debug, List<String> testGeneIDs) {
        super(Allele.class);
        this.debug = debug;
        this.testGeneIDs = testGeneIDs;
    }

    public AlleleRepository() {
        super(Allele.class);
    }

    public Allele getAllele(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";
        query += " MATCH p1=(aSpecies:Species)-[:FROM_SPECIES]-(a:Allele) WHERE a.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH p3=(a:Allele)-[:IS_ALLELE_OF]-(gene:Gene)-[:FROM_SPECIES]-(gSpecies:Species)";
        query += " OPTIONAL MATCH p4=(a:Allele)-[:ALSO_KNOWN_AS]-(:Synonym)";
        query += " OPTIONAL MATCH vari=(a:Allele)<-[:VARIATION]-(variant:Variant)-[:VARIATION_TYPE]-(soTerm:SOTerm)";
        query += " OPTIONAL MATCH crossRef=(a:Allele)-[:CROSS_REFERENCE]-(:CrossReference)";
        query += " OPTIONAL MATCH construct=(a:Allele)-[:CONTAINS]-(con:Construct)";
        query += " OPTIONAL MATCH crossRefCon=(con:Construct)-[:CROSS_REFERENCE]-(:CrossReference)";
        query += " OPTIONAL MATCH regGene=(con:Construct)<-[:IS_REGULATED_BY]-(:Gene)-[:FROM_SPECIES]->(:Species)";
        query += " OPTIONAL MATCH expGene=(con:Construct)-[:EXPRESSES]-(:Gene)-[:FROM_SPECIES]->(:Species)";
        query += " OPTIONAL MATCH expNonBgiCC=(con:Construct)-[:EXPRESSES]-(:NonBGIConstructComponent)";
        query += " OPTIONAL MATCH expNonBgiCCRegulation=(con:Construct)-[:IS_REGULATED_BY]-(:NonBGIConstructComponent)";
        query += " OPTIONAL MATCH expNonBgiCCTarget=(con:Construct)-[:TARGETS]-(:NonBGIConstructComponent)";
        query += " OPTIONAL MATCH targetGene=(con:Construct)-[:TARGETS]-(:Gene)-[:FROM_SPECIES]->(:Species)";
        query += " OPTIONAL MATCH p2=(gene:Gene)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome)";
        query += " RETURN p1, p2, p3, p4, crossRef, construct, regGene, vari, expGene, targetGene, crossRefCon, expNonBgiCC, expNonBgiCCRegulation, expNonBgiCCTarget";

        Iterable<Allele> alleles = query(query, map);
        for (Allele a : alleles) {
            if (a.getPrimaryKey().equals(primaryKey)) {
                return a;
            }
        }

        return null;
    }

    public List<String> getAllAlleleKeys() {
        String query = "MATCH (a:Allele)--(g:Gene)-[:FROM_SPECIES]-(q:Species) RETURN a.primaryKey";

        Result r = queryForResult(query);
        Iterator<Map<String, Object>> i = r.iterator();

        ArrayList<String> list = new ArrayList<>();

        while (i.hasNext()) {
            Map<String, Object> map2 = i.next();
            list.add((String) map2.get("a.primaryKey"));
        }
        return list;
    }

    private Map<String, Transcript> transcriptMap;
    // geneID, chromosome
    private Map<String, String> geneChromosomeMap = new HashMap<>();
    // allele ID for which disease info exists
    private Set<String> alleleDiseaseSet = new HashSet<>();
    // allele ID, disease info exists
    private Set<String> allelePhenoSet = new HashSet<>();

    public Map<String, Transcript> getTranscriptWithExonInfo() {
        if (MapUtils.isNotEmpty(transcriptMap))
            return transcriptMap;
        String query = "";
        // get Transcript - Exon relationships
        query += " MATCH p1=(t:Transcript)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome) ";
        query += " OPTIONAL MATCH p2=(:GenomicLocation)<-[:ASSOCIATION]-(:Exon)-[:EXON]->(t:Transcript) ";
        query += " RETURN p1, p2";
        Iterable<Transcript> transcriptExonsIter = query(Transcript.class, query);
        log.info("Number of Transcript/Exon relationships: " + String.format("%,d", (int) StreamSupport.stream(transcriptExonsIter.spliterator(), false).count()));
        transcriptMap = StreamSupport.stream(transcriptExonsIter.spliterator(), false)
                .collect(Collectors.toSet())
                .stream()
                .collect(Collectors.toMap(Transcript::getPrimaryKey, transcript -> transcript));
        return transcriptMap;
    }

    public Map<String, String> getGeneChromosomeInfo() {
        if (MapUtils.isNotEmpty(geneChromosomeMap))
            return geneChromosomeMap;

        String query = "";
        query += " MATCH p=(g:Gene)--(c:Chromosome)";
        query += " RETURN g.primaryKey, c.primaryKey ";
        Result map = queryForResult(query);

        StreamSupport.stream(map.spliterator(), false)
                .forEach(entrySet -> {
                    final Iterator<Map.Entry<String, Object>> iterator = entrySet.entrySet().iterator();
                    final Map.Entry<String, Object> entryKey = iterator.next();
                    final Map.Entry<String, Object> entryValue = iterator.next();
                    geneChromosomeMap.put((String) entryKey.getValue(), (String) entryValue.getValue());
                });

        query = " MATCH p=(g:Gene)";
        query += " where not exists ((g)-[:LOCATED_ON]->(:Chromosome)) ";
        query += " RETURN g.primaryKey ";
        map = queryForResult(query);

        StreamSupport.stream(map.spliterator(), false)
                .forEach(entrySet -> {
                    final Iterator<Map.Entry<String, Object>> iterator = entrySet.entrySet().iterator();
                    final Map.Entry<String, Object> entryValue = iterator.next();
                    geneChromosomeMap.put("", (String) entryValue.getValue());
                });

        log.info("Number of Gene/Chromosome relationships: " + String.format("%,d", geneChromosomeMap.size()));
        return geneChromosomeMap;
    }

    public boolean hasAlleleDiseaseInfo(String alleleID) {
        if (CollectionUtils.isNotEmpty(alleleDiseaseSet))
            return alleleDiseaseSet.contains(alleleID);

        String query = "";
        query += " MATCH (a:Allele)<-[:IS_IMPLICATED_IN]-(doTerm:DOTerm) ";
        query += " RETURN distinct(a.primaryKey) as ID ";
        Result result = queryForResult(query);
        StreamSupport.stream(result.spliterator(), false)
                .forEach(idMap -> {
                    alleleDiseaseSet.add((String) idMap.get("ID"));
                });
        log.info("Number of alleles with disease annotations: " + String.format("%,d", alleleDiseaseSet.size()));
        return alleleDiseaseSet.contains(alleleID);
    }

    public boolean hasAllelePhenoInfo(String alleleID) {
        if (CollectionUtils.isNotEmpty(allelePhenoSet))
            return allelePhenoSet.contains(alleleID);

        String query = "";
        query += " MATCH (a:Allele)-[:HAS_PHENOTYPE]->(ph:Phenotype) ";
        query += " RETURN distinct(a.primaryKey) as ID";
        Result result = queryForResult(query);
        StreamSupport.stream(result.spliterator(), false)
                .forEach(idMap -> {
                    allelePhenoSet.add((String) idMap.get("ID"));
                });
        log.info("Number of alleles with phenotype annotations: " + String.format("%,d", allelePhenoSet.size()));
        return allelePhenoSet.contains(alleleID);
    }

    public Set<Allele> getAlleles(String taxonID, String chromosome) {
        Map<String, Set<Allele>> map = getAllAlleles().get(taxonID);
        if (chromosome == null && map == null) {
            return new HashSet<>();
        }
        // return alleles for all chromosomes if no chromosome number is given
        if (chromosome == null) {
            return map.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        }
        if (map == null) {
            map = new HashMap<>();
        }
        log.info("taxon,chromo" + ": " + taxonID + "," + chromosome);
        final Set<Allele> alleles = new HashSet<>(map.computeIfAbsent(chromosome, s -> new HashSet<>()));
        log.info("Size" + ": " + alleles.size());
        return alleles;
    }


    private final Map<String, Map<String, Set<Allele>>> allAlleleMap = new HashMap<>();

    public Map<String, Map<String, Set<Allele>>> getAllAlleles() {
        if (MapUtils.isNotEmpty(allAlleleMap))
            return allAlleleMap;

        String query = "";
        // allele-only (no variants)
        query += " MATCH p1=(:Species)<-[:FROM_SPECIES]-(a:Allele)-[:IS_ALLELE_OF]->(g:Gene)-[:FROM_SPECIES]-(q:Species) ";
        query += "where not exists ((a)<-[:VARIATION]-(:Variant)) ";
        //query += " AND  g.primaryKey = 'WB:WBGene00000913' ";

//        query += " AND g.taxonId = 'NCBITaxon:7955' ";
//        query += " AND g.primaryKey = 'RGD:9294106' ";
        if (testGeneIDs != null) {
            StringJoiner joiner = new StringJoiner("','", "'", "'");
            testGeneIDs.forEach(joiner::add);
            String inClause = joiner.toString();
            query += " AND g.primaryKey in [" + inClause + "]";
        }
        query += " OPTIONAL MATCH p2=(a:Allele)-[:ALSO_KNOWN_AS]->(synonym:Synonym)";
        query += " OPTIONAL MATCH crossRef=(a:Allele)-[:CROSS_REFERENCE]->(c:CrossReference)";
        query += " RETURN p1, p2, crossRef ";

        Iterable<Allele> alleles = query(query, new HashMap<>());
        Set<Allele> allAlleles = StreamSupport.stream(alleles.spliterator(), false)
                .collect(Collectors.toSet());
        log.info("Number of alleles without variants: " + String.format("%,d", allAlleles.size()));

        // alleles with variant records
        query = "";
        query += " MATCH p1=(g:Gene)<-[:IS_ALLELE_OF]-(a:Allele)<-[:VARIATION]-(variant:Variant)--(:SOTerm) ";
        query += ", p0=(:Species)<-[:FROM_SPECIES]-(a:Allele) ";
//        query += " where g.taxon955' ";
        if (testGeneIDs != null) {
            StringJoiner joiner = new StringJoiner("','", "'", "'");
            testGeneIDs.forEach(joiner::add);
            String inClause = joiner.toString();
            query += " WHERE g.primaryKey in [" + inClause + "]";
        }
/*
        if (testGeneIDs == null)
            query += " where  a.primaryKey = 'SGD:S000281388' ";
        else
            query += " AND  a.primaryKey = 'SGD:S000281388' ";
*/

        query += " OPTIONAL MATCH consequence = (t:Transcript)--(:TranscriptLevelConsequence)--(variant:Variant)<-[:ASSOCIATION]-(t:Transcript)--(:SOTerm) ";
        query += " OPTIONAL MATCH loc=(variant:Variant)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome)";
        query += " OPTIONAL MATCH p2=(a:Allele)-[:ALSO_KNOWN_AS]->(synonym:Synonym)";
        query += " OPTIONAL MATCH crossRef=(a:Allele)-[:CROSS_REFERENCE]->(c:CrossReference)";
        query += " RETURN p0, p1, p2, consequence, loc, crossRef ";
        Iterable<Allele> allelesWithVariantsIter = query(query, new HashMap<>());
        Set<Allele> allelesWithVariants = StreamSupport.stream(allelesWithVariantsIter.spliterator(), false)
                .collect(Collectors.toSet());
        log.info("Number of alleles with variants: " + String.format("%,d", allelesWithVariants.size()));
        // fixup transcripts with genomic location and exon information
        fixupAllelesWithVariants(allAlleles, allelesWithVariants);

        Set<Allele> allAlleleSet = new HashSet<>(allAlleles);

        // group by taxon ID
        Map<String, List<Allele>> taxonMap = allAlleleSet.stream().collect(groupingBy(allele -> allele.getGene().getTaxonId()));
        taxonMap.forEach((taxonID, alleleList) -> {
            Set<String> chromosomes = alleleList.stream()
                    .filter(allele -> getGeneChromosomeInfo().get(allele.getGene().getPrimaryKey()) != null)
                    .map(allele -> getGeneChromosomeInfo().get(allele.getGene().getPrimaryKey()))
                    .collect(Collectors.toSet());
            // unknown chromosome
            chromosomes.add("");

            Map<String, Set<Allele>> chromosomeMap = new HashMap<>();
            // group by chromosome number
            chromosomes.forEach(chromosome -> {
                Set<Allele> chromosomeAlleles = new HashSet<>();

                // all alleles with chromosome info
                alleleList.stream()
                        .filter(allele -> getGeneChromosomeInfo().get(allele.getGene().getPrimaryKey()) != null)
                        .filter(allele -> getGeneChromosomeInfo().get(allele.getGene().getPrimaryKey()).equals(chromosome))
                        .forEach(chromosomeAlleles::add);
                chromosomeMap.put(chromosome, chromosomeAlleles);
            });
            allAlleleMap.put(taxonID, chromosomeMap);
        });

        // fixup the chromosome info on the gene object
        // including the chromosome in the cypher query make the query take very long.
        allAlleleMap.forEach((taxonID, map) -> {
            map.values().forEach(alleleList -> {
                alleleList.forEach(allele -> {
                    Chromosome chromosome = new Chromosome();
                    chromosome.setPrimaryKey(allele.getGene().getPrimaryKey());
                    allele.getGene().setChromsomes(List.of(chromosome));
                });
            });
        });
        allAlleleMap.forEach((taxonID, map) -> {
            Map<String, Integer> stats = map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
            log.info(taxonID + ": " + stats);
        });
        return allAlleleMap;
    }
    public Map<String, List<Allele>> getAllAllelesByTaxonNChromosome(String taxonId, String chr) {
       /* taxonId="NCBITaxon:10116";
        chr="12";*/
        String query="MATCH p1=(:SOTerm)--(v:Variant)-[:VARIATION]->" +
                "(a:Allele{taxonId: \""+taxonId+"\"})-[:IS_ALLELE_OF]->(g:Gene{taxonId: \""+taxonId+"\"})" +
                "-[r:LOCATED_ON]->(c:Chromosome{primaryKey:\""+chr+"\"}) ";


        query += " OPTIONAL MATCH consequence = (t:Transcript)--(:TranscriptLevelConsequence)--(v:Variant)<-[:ASSOCIATION]-(t:Transcript)--(:SOTerm) ";
        query += " OPTIONAL MATCH loc=(v:Variant)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome)";
        query += " OPTIONAL MATCH p2=(a:Allele)-[:ALSO_KNOWN_AS]->(synonym:Synonym)";
        query += " OPTIONAL MATCH crossRef=(a:Allele)-[:CROSS_REFERENCE]->(c:CrossReference)";
        query += " RETURN p1, p2, consequence, loc, crossRef ";
      //  query += " RETURN p1, consequence, loc  ";
        System.out.println(query);
        Iterable<Allele> allelesWithVariantsIter = query(query, new HashMap<>());
        Set<Allele> allelesWithVariants = StreamSupport.stream(allelesWithVariantsIter.spliterator(), false)
                .collect(Collectors.toSet());
        Map<String, List<Allele>> allelesMap=new HashMap<>();
        for(Allele a: allelesWithVariants){
         List<Variant> variants=   a.getVariants();
         for(Variant v:variants){
             List<Allele> alleles=new ArrayList<>();
             if(allelesMap.get(v.getHgvsG().get(1))!=null && allelesMap.get(v.getHgvsG().get(1)).size()>0){
                 alleles.addAll(allelesMap.get(v.getHgvsG().get(1)));
             }
             alleles.add(a);
             allelesMap.put(v.getHgvsG().get(1), alleles);
         }
        }
        log.info("Number of alleles with variants: " + String.format("%,d", allelesWithVariants.size()));

        return allelesMap;
    }

    public void fixupAllelesWithVariants(Set<Allele> allAlleles, Set<Allele> allelesWithVariants) {
        allelesWithVariants.forEach(allele -> {
            allele.getVariants().stream()
                    .filter(Objects::nonNull)
                    .filter(variant -> variant.getTranscriptList() != null)
                    .forEach(variant ->
                            variant.getTranscriptList().forEach(transcript -> {
                                if (!debug) {
                                    final Transcript transcript1 = getTranscriptWithExonInfo().get(transcript.getPrimaryKey());
                                    if (transcript1 != null) {
                                        if (transcript1.getGenomeLocation() != null)
                                            transcript.setGenomeLocation(transcript1.getGenomeLocation());
                                        if (transcript1.getExons() != null)
                                            transcript.setExons(transcript1.getExons());
                                    }
                                }
                            }));
            allele.setDisease(hasAlleleDiseaseInfo(allele.getPrimaryKey()));
            allele.setPhenotype(hasAllelePhenoInfo(allele.getPrimaryKey()));
        });
        allAlleles.addAll(allelesWithVariants);
        allAlleles.forEach(Allele::populateCategory);
    }

    /*
     * Need to run 3 queries as a union as the where clause is against the gene of
     * three typed associations (EXPRESSES, TARGET, IS_REGULATED_BY) against the construct node
     * but there might be more genes with the same association
     */
    public List<Allele> getTransgenicAlleles(String geneID) {
        HashMap<String, String> map = new HashMap<>();
        map.put("geneID", geneID);
        String query = getCypherQuery("EXPRESSES");

        Iterable<Allele> alleles = query(query, map);
        List<Allele> alleleList = StreamSupport.stream(alleles.spliterator(), false)
                .collect(Collectors.toList());

        query = getCypherQuery("TARGETS");

        alleles = query(query, map);
        alleleList.addAll(StreamSupport.stream(alleles.spliterator(), false)
                .collect(Collectors.toList()));

        alleleList.sort(Comparator.comparing(Allele::getSymbolText));
        return alleleList;
    }

    private String getCypherQuery(String relationship) {
        String query = "";
        query += " MATCH p1=(:Species)<-[:FROM_SPECIES]-(allele:Allele)--(construct:Construct)-[:" + relationship + "]-(gene:Gene)--(:Species) " +
                "  where gene.primaryKey = {geneID}";
        // need this optional match to retrieve all expresses genes besides the given geneID
        query += " OPTIONAL MATCH express=(:CrossReference)--(construct:Construct)-[:EXPRESSES]-(:Gene)--(:Species)";
        query += " OPTIONAL MATCH expressNonBGI=(:CrossReference)--(construct:Construct)-[:EXPRESSES]-(:NonBGIConstructComponent)";
        query += " OPTIONAL MATCH target=(:CrossReference)--(construct:Construct)-[:TARGET]-(:Gene)--(:Species)";
        query += " OPTIONAL MATCH targetNon=(:CrossReference)--(construct:Construct)-[:TARGET]-(:NonBGIConstructComponent)";
        query += " OPTIONAL MATCH regulated=(:CrossReference)--(construct:Construct)-[:IS_REGULATED_BY]-(:Gene)--(:Species)";
        query += " OPTIONAL MATCH regulatedNon=(:CrossReference)--(construct:Construct)-[:IS_REGULATED_BY]-(:NonBGIConstructComponent)";
        query += " OPTIONAL MATCH disease=(allele:Allele)--(:DiseaseEntityJoin)";
        query += " OPTIONAL MATCH pheno=(allele:Allele)-[:HAS_PHENOTYPE]-(:Phenotype)";
        query += " RETURN p1, express, target, regulated, expressNonBGI, regulatedNon, targetNon, disease, pheno ";
        return query;
    }

    public Set<Allele> getAllAlleleVariantInfoOnGene() {
        HashMap<String, String> map = new HashMap<>();

        String query = "";
        query += " MATCH p1=(:Species)<-[:FROM_SPECIES]-(a:Allele)-[:IS_ALLELE_OF]->(g:Gene),  ";

/*
        query += " where g.primaryKey = 'ZFIN:ZDB-GENE-001212-1' ";
        query += " AND  a.primaryKey = 'ZFIN:ZDB-ALT-130411-1942' ";
*/
        query += " OPTIONAL MATCH vari=(a:Allele)<-[:VARIATION]-(variant:Variant)-[:VARIATION_TYPE]->(soTerm:SOTerm)";
        query += " OPTIONAL MATCH consequence=(variant:Variant)-[:ASSOCIATION]->(:GeneLevelConsequence)";
        query += " OPTIONAL MATCH loc=(variant:Variant)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome)";
        query += " OPTIONAL MATCH p2=(a:Allele)-[:ALSO_KNOWN_AS]->(synonym:Synonym)";
        query += " OPTIONAL MATCH crossRef=(a:Allele)-[:CROSS_REFERENCE]->(c:CrossReference)";
        query += " OPTIONAL MATCH variantPub=(a:Allele)<-[:VARIATION]-(variant:Variant)-[:ASSOCIATION]->(:Publication)";
/*
        query += " OPTIONAL MATCH disease=(a:Allele)<-[:IS_IMPLICATED_IN]-(doTerm:DOTerm)";
        query += " OPTIONAL MATCH pheno=(a:Allele)-[:HAS_PHENOTYPE]->(ph:Phenotype)";
*/
        query += " OPTIONAL MATCH transcript=(a:Allele)<-[:VARIATION]-(variant:Variant)<-[:ASSOCIATION]-(:Transcript)--(:TranscriptLevelConsequence)--(variant:Variant)";
        query += " OPTIONAL MATCH transcriptType=(a:Allele)<-[:VARIATION]-(variant:Variant)<-[:ASSOCIATION]-(:Transcript)--(:SOTerm)";
        query += " OPTIONAL MATCH transcriptLocation=(:GenomicLocation)<-[:ASSOCIATION]-(:Exon)-[:EXON]->(t:Transcript)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome), (a:Allele)<-[:VARIATION]-(:Variant)<-[:ASSOCIATION]-(t:Transcript)";
        query += " RETURN p1, p2, vari, crossRef, loc, consequence, transcript, variantPub, transcriptLocation, transcriptType ";

        Iterable<Allele> alleles = query(query, map);
        return StreamSupport.stream(alleles.spliterator(), false)
                .collect(Collectors.toSet());
    }
}
