package org.alliancegenome.api

import org.alliancegenome.api.tests.integration.ApiTester
import spock.lang.Specification
import spock.lang.Unroll

class TransgenicAlleleIntegrationSpec extends Specification {

    @Unroll
    def "Gene page - Transgenic Alleles for #geneId and #filterAllele"() {
        when:
        def results = ApiTester.getApiResult("/api/gene/$geneId/transgenic-alleles?limit=5&filter.allele=$filterAllele")

        then:
        results
        totalSize == results.total

        where:
        geneId           | filterAllele | totalSize
        "FB:FBgn0284084" | ""           | 141
        "FB:FBgn0284084" | "frt"        | 33
    }

    @Unroll
    def "Gene page - Transgenic Alleles for #geneId and #construct "() {
        when:
        def results = ApiTester.getApiResult("/api/gene/$geneId/transgenic-alleles?filter.construct=$construct")

        then:
        results
        totalSize == results.total

        where:
        geneId           | construct | totalSize
        "FB:FBgn0284084" | ""        | 141
        "FB:FBgn0284084" | "uas"     | 73
    }

    @Unroll
    def "Gene page - Transgenic Alleles for #geneId and #constructExpressedGenes "() {
        when:
        def results = ApiTester.getApiResult("/api/gene/$geneId/transgenic-alleles?filter.constructExpressedGene=$constructExpressedGenes")

        then:
        results
        totalSize == results.total

        where:
        geneId           | constructExpressedGenes | totalSize
        "FB:FBgn0284084" | ""                      | 141
        "FB:FBgn0284084" | "tag"                   | 37
    }

    @Unroll
    def "Gene page - Transgenic Alleles for #geneId and #constructRegulatedGenes "() {
        when:
        def results = ApiTester.getApiResult("/api/gene/$geneId/transgenic-alleles?filter.constructRegulatedGene=$constructRegulatedGenes")

        then:
        results
        totalSize == results.total

        where:
        geneId           | constructRegulatedGenes | totalSize
        "FB:FBgn0284084" | ""                      | 141
        "FB:FBgn0284084" | "hsp"                   | 30
    }

    @Unroll
    def "Gene page - Transgenic Alleles for #geneId and #constructTargetedGenes "() {
        when:
        def results = ApiTester.getApiResult("/api/gene/$geneId/transgenic-alleles?filter.constructTargetedGene=$constructTargetedGenes")

        then:
        results
        totalSize == results.total

        where:
        geneId           | constructTargetedGenes | totalSize
        "FB:FBgn0284084" | ""                     | 141
        "FB:FBgn0284084" | "wg"                   | 14
    }

/*
    @Unroll
    def "Gene page - Transgenic Alleles for #geneId "() {
        when:
        def results = ApiTester.getApiResult("/api/gene/$geneId/transgenic-alleles?limit=5")

        then:
        results
        totalSize == results.supplewmentalData

        where:
        geneId           | filterAllele | totalSize
        "FB:FBgn0284084" | ""           | 141
        "FB:FBgn0284084" | "frt"        | 33
    }

*/
/*
    @Unroll
    def "Gene page - Transgenic Alleles for #geneId"() {
        when:
        def results = ApiTester.getApiResults("/api/gene/$geneId/transgenic-alleles?limit=5&filter.allele=frt")

        def constructNames = results.constructs.findAll { it }

        then:
        results
        totalSize <= results.size()
        phenotype == constructNames.join(',')

        where:
        geneId           | totalSize | phenotype
        "FB:FBgn0284084" | 10        | "anatomical system quality, abnormal,ansulate commissure decreased size, abnormal,apoptotic process increased occurrence, abnormal,blood circulation disrupted, abnormal,brain hydrocephalic, abnormal,brain lacks all parts of type midbrain hindbrain boundary, abnormal,brain quality, abnormal,caudal commissure increased size, abnormal,cerebellum absent, abnormal,cerebellum malformed, abnormal"
    }

*/
}