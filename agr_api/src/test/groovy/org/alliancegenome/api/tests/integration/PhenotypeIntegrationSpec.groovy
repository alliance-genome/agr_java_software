package org.alliancegenome.api

import spock.lang.Unroll

class PhenotypeIntegrationSpec extends AbstractSpec {

    @Unroll
    def "Gene page - Phenotype Annotations for #geneId"() {
        when:
        def results = getApiResults("/api/gene/$geneId/phenotypes?page=1&limit=10&sortBy=")

        def phenotypeNames = results.phenotype.findAll { it }

        then:
        results
        totalSize <= results.size()
        phenotype == phenotypeNames.join(',')

        where:
        geneId                   | totalSize | phenotype
        "ZFIN:ZDB-GENE-990415-8" | 10        | "anatomical system quality, abnormal,anatomical system quality, abnormal,anatomical system quality, abnormal,anatomical system quality, abnormal,anatomical system quality, abnormal,anatomical system quality, abnormal,anatomical system quality, abnormal,ansulate commissure decreased size, abnormal,ansulate commissure decreased size, abnormal,apoptotic process increased occurrence, abnormal"
        "MGI:88052"              | 10        | "abnormal blood homeostasis,abnormal blood homeostasis,abnormal cholesterol homeostasis,abnormal cholesterol homeostasis,abnormal cholesterol homeostasis,abnormal circulating cholesterol level,abnormal circulating cholesterol level,abnormal circulating cholesterol level,abnormal circulating HDL cholesterol level,abnormal circulating HDL cholesterol level"
    }

    @Unroll
    def "Gene page - Phenotype Annotation Download for #geneId"() {
        when:
        def output = getApiResultRaw("/api/gene/$geneId/phenotypes/download?limit=100000")
        def lines = output.split("\n")
        then:
        lines
        headerLine == lines[0]
        lineOne == lines[1]
        totalSize <= lines.length
        where:
        geneId       | totalSize | lineOne                                                                                | headerLine
        "MGI:105043" | 300       | "abnormal atrial thrombosis\tMGI:2151800\tAhr<sup>tm1Gonz</sup>\tallele\tPMID:9396142" | "Phenotype\tGenetic Entity ID\tGenetic Entity Symbol\tGenetic Entity Type\tReferences"
        "MGI:109583" | 1200      | "abnormal adipose tissue morphology\t\t\tgene\tPMID:22405073"                          | "Phenotype\tGenetic Entity ID\tGenetic Entity Symbol\tGenetic Entity Type\tReferences"
    }

    @Unroll
    def "Gene page - Sort phenotype by phenotype for #geneId"() {
        when:
        def encodedGeneID = URLEncoder.encode(geneId, "UTF-8")
        def result = getApiResult("/api/gene/$encodedGeneID/phenotypes")

        then:
        result
        phenotype == result.results[0].phenotype
        geneticEntity == result.results[0].geneticEntity.symbol

        where:
        geneId              | phenotype                            | geneticEntity
        "WB:WBGene00000898" | "aging variant"                      | "e1370"
        "MGI:109583"        | "abnormal adipose tissue morphology" | "Pten"

    }

    @Unroll
    def "Gene page - Sort phenotype by genetic entity for #geneId"() {
        when:
        def encodedGeneID = URLEncoder.encode(geneId, "UTF-8")
        def result = getApiResult("/api/gene/$encodedGeneID/phenotypes?sortBy=geneticEntity")

        then:
        result
        phenotype == result.results[0].phenotype
        geneticEntity == result.results[0].geneticEntity.symbol

        where:
        geneId              | phenotype            | geneticEntity
        "WB:WBGene00000898" | "dauer constitutive" | "e979"
        "MGI:109583"        | "cardia bifida"      | "Pten<sup>m1un</sup>"

    }

}