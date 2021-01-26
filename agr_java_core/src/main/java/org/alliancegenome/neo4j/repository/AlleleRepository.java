package org.alliancegenome.neo4j.repository;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Chromosome;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.apache.commons.collections4.MapUtils;
import org.neo4j.ogm.model.Result;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;

@Log4j2
public class AlleleRepository extends Neo4jRepository<Allele> {

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

    public Set<Allele> getAlleles(String taxonID, String chromosome) {
        // return alleles for all chromosomes
        Map<String, List<Allele>> map = getAllAlleles().get(taxonID);
        if (chromosome == null && map == null) {
            return new HashSet<>();
        }
        if (chromosome == null) {
            return map.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        }
        if (map == null) {
            map = new HashMap<>();
        }
        List<Allele> alleles = map.get(chromosome);
        return alleles != null ? new HashSet<>(alleles) : new HashSet<>();
    }


    private final Map<String, Map<String, List<Allele>>> allAlleleMap = new HashMap<>();

    public Map<String, Map<String, List<Allele>>> getAllAlleles() {
        if (MapUtils.isNotEmpty(allAlleleMap))
            return allAlleleMap;

        String query = "";
        // allele-only (no variants)
        query += " MATCH p1=(:Species)<-[:FROM_SPECIES]-(a:Allele)-[:IS_ALLELE_OF]->(g:Gene)-[:FROM_SPECIES]-(q:Species) ";
        query += ", p3=(g:Gene)--(:Chromosome) ";
        query += "where not exists ((a)<-[:VARIATION]-(:Variant)) ";
        //query += " AND  g.primaryKey = 'WB:WBGene00000913' ";

//        query += " AND g.taxonId = 'NCBITaxon:7955' ";
//        query += " AND g.primaryKey = 'RGD:9294106' ";
//        query += " AND g.primaryKey in ['RGD:9294106', 'ZFIN:ZDB-GENE-001212-1', 'WB:WBGene00000913', 'ZFIN:ZDB-GENE-011101-3'] ";
        query += " OPTIONAL MATCH disease=(a:Allele)<-[:IS_IMPLICATED_IN]-(doTerm:DOTerm)";
        query += " OPTIONAL MATCH pheno=(a:Allele)-[:HAS_PHENOTYPE]->(ph:Phenotype)";
        query += " OPTIONAL MATCH p2=(a:Allele)-[:ALSO_KNOWN_AS]->(synonym:Synonym)";
        query += " OPTIONAL MATCH crossRef=(a:Allele)-[:CROSS_REFERENCE]->(c:CrossReference)";
        query += " RETURN p1, p2, p3, disease, pheno, crossRef ";

        Iterable<Allele> alleles = query(query, new HashMap<>());
        Set<Allele> allAlleles = StreamSupport.stream(alleles.spliterator(), false)
                .collect(Collectors.toSet());
        log.info("Number of alleles without variants: " + String.format("%,d", allAlleles.size()));

        // alleles with variant records
        query = "";
        query += " MATCH p1=(g:Gene)<-[:IS_ALLELE_OF]-(a:Allele)<-[:VARIATION]-(variant:Variant)--(:SOTerm) ";
        query += ", p0=(:Species)<-[:FROM_SPECIES]-(a:Allele) ";
        query += ", p3=(g:Gene)--(:Chromosome) ";
//        query += " where g.taxonId = 'NCBITaxon:7955' ";
//        query += " AND g.primaryKey in [ 'ZFIN:ZDB-GENE-001212-1', 'ZFIN:ZDB-GENE-011101-3'] ";
//        query += " where g.primaryKey = 'WB:WBGene00000913' ";
//        query += " AND  a.primaryKey = 'ZFIN:ZDB-ALT-130411-1942' ";

        query += " OPTIONAL MATCH consequence = (t:Transcript)--(:TranscriptLevelConsequence)--(variant:Variant)<-[:ASSOCIATION]-(t:Transcript)--(:SOTerm) ";
        query += " OPTIONAL MATCH loc=(variant:Variant)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome)";
        query += " OPTIONAL MATCH p2=(a:Allele)-[:ALSO_KNOWN_AS]->(synonym:Synonym)";
        query += " OPTIONAL MATCH disease=(a:Allele)<-[:IS_IMPLICATED_IN]-(doTerm:DOTerm)";
        query += " OPTIONAL MATCH pheno=(a:Allele)-[:HAS_PHENOTYPE]->(ph:Phenotype)";
        query += " OPTIONAL MATCH crossRef=(a:Allele)-[:CROSS_REFERENCE]->(c:CrossReference)";
        query += " RETURN p0, p1, p2, p3, consequence, loc, pheno, disease, crossRef ";
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
                    .filter(allele -> allele.getGene().getChromsomes() != null)
                    .map(allele -> allele.getGene().getChromsomes())
                    .flatMap(Collection::stream)
                    .map(Chromosome::getPrimaryKey)
                    .collect(Collectors.toSet());
            // unknown chromosome
            chromosomes.add("");

            // group by chromosome number
            chromosomes.forEach(chromosome -> {
                Map<String, List<Allele>> chromosomeMap = allAlleleMap.computeIfAbsent(taxonID, k -> new HashMap<>());
                List<Allele> chromosomeAlleles = chromosomeMap.computeIfAbsent(chromosome, k -> new ArrayList<>());

                // all alleles with chromosome info
                alleleList.stream()
                        .filter(allele -> allele.getGene().getChromsomes() != null)
                        .filter(allele -> allele.getGene().getChromsomes()
                                .stream()
                                .map(Chromosome::getPrimaryKey)
                                .anyMatch(chr -> chr.equals(chromosome)))
                        .forEach(chromosomeAlleles::add);
                // all alleles without chromosome info
                alleleList.stream()
                        .filter(allele -> allele.getGene().getGenomeLocations() == null)
                        .forEach(chromosomeAlleles::add);
            });
        });

        allAlleleMap.forEach((taxonID, map) -> {
            Map<String, Integer> stats = map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
            log.info(taxonID + ": " + stats);
        });
        return allAlleleMap;
    }

    public void fixupAllelesWithVariants(Set<Allele> allAlleles, Set<Allele> allelesWithVariants) {
        allelesWithVariants.forEach(allele ->
                allele.getVariants().stream()
                        .filter(Objects::nonNull)
                        .filter(variant -> variant.getTranscriptList() != null)
                        .forEach(variant ->
                                variant.getTranscriptList().forEach(transcript -> {
                                    final Transcript transcript1 = getTranscriptWithExonInfo().get(transcript.getPrimaryKey());
                                    if (transcript1 != null) {
                                        if (transcript1.getGenomeLocation() != null)
                                            transcript.setGenomeLocation(transcript1.getGenomeLocation());
                                        if (transcript1.getExons() != null)
                                            transcript.setExons(transcript1.getExons());
                                    }
                                })));
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
