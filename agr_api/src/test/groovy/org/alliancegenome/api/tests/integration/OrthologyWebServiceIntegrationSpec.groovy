package org.alliancegenome.api

import spock.lang.Ignore
import spock.lang.Unroll

class OrthologyWebServiceIntegrationSpec extends AbstractSpec {

    @Ignore("Not working until we get test data into travis")
    @Unroll
    def "When querying for #species "() {
        when:
        def result = getApiResult("/api/homologs/$species")
        def results = result.results

        def total = result.total
        def Set<String> genePair = new HashSet<>()
        def Set<String> geneIdPair = new HashSet<>()
        for (Map map : results) {
            String gene = map.get("gene").get("symbol")
            String homologGene = map.get("homologGene").get("symbol")
            String geneId = map.get("gene").get("primaryKey")
            String homologGeneId = map.get("homologGene").get("primaryKey")
            genePair.add(gene + " | " + homologGene);
            geneIdPair.add(geneId + " | " + homologGeneId);
        }
        then:
        total > numberOfRecords
        genePair.contains(pairName)
        geneIdPair.contains(pairID)

        where:
        species | numberOfRecords | pairName         | pairID
        "10090" | 20              | "Fscn2 | fscn2b" | "MGI:109583 | ZFIN:ZDB-GENE-030616-47"
    }

    @Ignore("Not working until we get test data into travis")
    @Unroll
    def "When querying for #species1 and #species2 "() {
        when:
        def results = getApiResults("/api/homologs/$species1/$species2")
        def total = results.total
        Set<String> genePair = new HashSet<>()
        Set<String> geneIdPair = new HashSet<>()
        for (Map map : results) {
            String gene = map.get("gene").get("symbol")
            String homologGene = map.get("homologGene").get("symbol")
            String geneId = map.get("gene").get("primaryKey")
            String homologGeneId = map.get("homologGene").get("primaryKey")
            genePair.add(gene + " | " + homologGene);
            geneIdPair.add(geneId + " | " + homologGeneId);
        }
        then:
        total > numberOfRecords
        genePair.contains(pairName)
        geneIdPair.contains(pairID)

        where:
        species1 | species2 | numberOfRecords | pairName      | pairID
        "10090"  | "10116"  | 5               | "Pten | Pten" | "RGD:61995 | MGI:109583"
    }

    @Unroll
    def "When querying for #geneID, #taxonID and #filter "() {
        when:
        def result = getApiResult("/api/gene/$geneID/homologs?stringencyFilter=$filter&start=$start&rows=$rows&taxonID=$taxonID")
        def total = result.total
        Set<String> genePair = new HashSet<>()
        Set<String> geneIdPair = new HashSet<>()
        result.results.each {
            genePair.add(it.gene.symbol + " | " + it.homologGene.symbol);
            geneIdPair.add(it.gene.geneID + " | " + it.homologGene.geneID);
        }

        then:
        total > lowerCount
        genePair.contains(pairName)
        geneIdPair.contains(pairID)

        where:
        geneID       | filter      | start | rows | taxonID          | lowerCount | pairName       | pairID
        "MGI:109583" | ""          | 1     | 100  | ""               | 15         | "Pten | Pten"  | "MGI:109583 | RGD:61995"
        "MGI:109583" | ""          | 1     | 7    | ""               | 6          | "Pten | Pten"  | "MGI:109583 | HGNC:21616"
        "MGI:109583" | "stringent" | 1     | 100  | ""               | 6          | "Pten | Pten"  | "MGI:109583 | ZFIN:ZDB-GENE-030616-47"
        "MGI:109583" | "stringent" | 1     | 100  | "NCBITaxon:7955" | 1          | "Pten | ptena" | "MGI:109583 | ZFIN:ZDB-GENE-030616-47"
    }

}