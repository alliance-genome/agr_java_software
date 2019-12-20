package org.alliancegenome.api

import org.alliancegenome.api.tests.integration.ApiTester
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class OrthologyWebServiceIntegrationSpec extends Specification {

    @Unroll
    def "Homologs for species #species "() {
        when:
        def result = ApiTester.getApiResult("/api/homologs/$species")

        def total = result.total
        Set<String> genePair = new HashSet<>()
        Set<String> geneIdPair = new HashSet<>()
        result.results.each {
            genePair.add(it.gene.symbol + " | " + it.homologGene.symbol);
            geneIdPair.add(it.gene.id + " | " + it.homologGene.id);
        }

        then:
        total > numberOfRecords

        where:
        species | numberOfRecords
        "10090" | 20
        "danio" | 20
    }

    @Ignore("Not working until we get test data into travis")
    @Unroll
    def "When querying for #species1 and #species2 "() {
        when:
        def results = ApiTester.getApiResults("/api/homologs/$species1/$species2")
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
        def result = ApiTester.getApiResult("/api/gene/$geneID/homologs?stringencyFilter=$filter&start=$start&rows=$rows")
        def total = result.total
        Set<String> genePair = new HashSet<>()
        Set<String> geneIdPair = new HashSet<>()
        result.results.each {
            genePair.add(it.gene.symbol + " | " + it.homologGene.symbol);
            geneIdPair.add(it.gene.id + " | " + it.homologGene.id);
        }

        then:
        total > lowerCount
        genePair.contains(pairName)
        geneIdPair.contains(pairID)

        where:
        geneID       | filter      | start | rows | taxonID | lowerCount | pairName      | pairID
        "MGI:109583" | ""          | 1     | 1000 | ""      | 15         | "Pten | Pten" | "MGI:109583 | RGD:61995"
        "MGI:109583" | "stringent" | 1     | 1000 | ""      | 6          | "Pten | Pten" | "MGI:109583 | ZFIN:ZDB-GENE-030616-47"
    }

}