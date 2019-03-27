package org.alliancegenome.api

import spock.lang.Ignore
import spock.lang.Unroll

class AlleleWebServiceIntegrationSpec extends AbstractSpec {

    @Ignore("Not working until we get test data into travis")
    @Unroll
    def "Homologs for species #species "() {
        when:
        def result = getApiResult("/api/homologs/$species")

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

    @Unroll
    def "Alleles for gene #geneID"() {
        when:
        def result = getApiResult("/api/gene/$geneID/alleles")
        def total = result.total
        Set<String> alleles = new HashSet<>()
        result.results.each {
            alleles.add(it.symbol);
        }

        then:
        total > lowerCount
        alleles.contains(alleleName)

        where:
        geneID       | lowerCount | alleleName
        "MGI:109583" | 15         | "Pten<sup>tm1.1Mro</sup>"
    }

}