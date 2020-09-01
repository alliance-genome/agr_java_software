package org.alliancegenome.api

import org.alliancegenome.api.tests.integration.ApiTester
import spock.lang.Specification
import spock.lang.Unroll

class PhenotypeIntegrationSpec extends Specification {

    @Unroll
    def "Gene page - Phenotype Annotations for #geneId"() {
        when:
        def results = ApiTester.getApiResults("/api/gene/$geneId/phenotypes?page=1&limit=10&sortBy=")

        def phenotypeNames = results.phenotype.findAll { it }

        then:
        results
        totalSize <= results.size()
        phenotype == phenotypeNames.join(',')

        where:
        geneId                   | totalSize | phenotype
        "ZFIN:ZDB-GENE-990415-8" | 10        | "anatomical system quality, abnormal,ansulate commissure decreased size, abnormal,apoptotic process increased occurrence, abnormal,blood circulation disrupted, abnormal,brain hydrocephalic, abnormal,brain lacks all parts of type midbrain hindbrain boundary, abnormal,brain quality, abnormal,caudal commissure increased size, abnormal,cerebellum absent, abnormal,cerebellum malformed, abnormal"
        "MGI:88052"              | 10        | "abnormal blood homeostasis,abnormal cholesterol homeostasis,abnormal circulating cholesterol level,abnormal circulating HDL cholesterol level,abnormal embryo development,abnormal triglyceride level,abnormal visceral yolk sac endoderm morphology,asthenozoospermia,decreased circulating cholesterol level,decreased circulating HDL cholesterol level"
    }

    @Unroll
    def "Gene page - Phenotype Annotation Download for #geneId"() {
        when:
        def output = ApiTester.getApiResultRaw("/api/gene/$geneId/phenotypes/download?limit=100000")
        def lines = output.split("\n")
        then:
        lines
        headerLine == lines[0]
        lineOne == lines[1]
        totalSize <= lines.length
        where:
        geneId       | totalSize | lineOne                                             | headerLine
        "MGI:105043" | 134       | "abnormal atrial thrombosis\tMGI:2450836\tAhr<tm1Gonz>/Ahr<tm1Gonz> [background:] involves: 129S4/SvJae * C57BL/6N\tgenotype\tMGI\tPMID:9396142"          | "Phenotype\tGenetic Entity ID\tGenetic Entity Name\tGenetic Entity Type\tSource\tReference"
        "MGI:109583" | 516       | "abnormal adipose tissue morphology\tMGI:5796492\tTg(Pten)1Srn/? [background:] involves: C57BL/6 * CBA\tgenotype\tMGI\tPMID:22405073" | "Phenotype\tGenetic Entity ID\tGenetic Entity Name\tGenetic Entity Type\tSource\tReference"
    }

    @Unroll
    def "Gene page - Sort phenotype by phenotype for #geneId"() {
        when:
        def encodedGeneID = URLEncoder.encode(geneId, "UTF-8")
        def result = ApiTester.getApiResult("/api/gene/$encodedGeneID/phenotypes")

        then:
        result
        phenotype == result.results[0].phenotype
        geneticEntity == result.results[0].primaryAnnotatedEntities[0].name

        where:
        geneId              | phenotype       | geneticEntity
        "WB:WBGene00000898" | "aging variant" | "e1370"

    }

/*
    @Unroll
    def "Gene page - Sort phenotype by genetic entity for #geneId"() {
        when:
        def encodedGeneID = URLEncoder.encode(geneId, "UTF-8")
        def result = getApiResult("/api/gene/$encodedGeneID/phenotypes?sortBy=geneticEntity")

        then:
        result
        phenotype == result.results[0].phenotype
        geneticEntity == result.results[0].allele.symbol

        where:
        geneId              | phenotype            | geneticEntity
        "WB:WBGene00000898" | "dauer constitutive" | "e979"
        "MGI:109583"        | "cardia bifida"      | "Pten<sup>m1un</sup>"

    }

*/
}