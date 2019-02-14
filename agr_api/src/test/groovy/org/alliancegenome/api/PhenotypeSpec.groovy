package org.alliancegenome.api

import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll

class PhenotypeSpec extends Specification {

    @Unroll
    def "Gene page - Phenotype Annotations for #geneId"() {
        when:
        def url = new URL("http://localhost:8080/api/gene/$geneId/phenotypes?page=1&limit=10&sortBy=")
        def retObj = new JsonSlurper().parseText(url.text)
        def results = retObj.results
        def phenotypeNames = results.phenotype.findAll { it }

        then:
        results
        totalSize <= results.size()
        phenotype == phenotypeNames.join(',')

        where:
        geneId                           | totalSize | phenotype
        "ZFIN:ZDB-GENE-990415-8"         | 10        | "anatomical system quality, abnormal,anatomical system quality, abnormal,anatomical system quality, abnormal,anatomical system quality, abnormal,anatomical system quality, abnormal,anatomical system quality, abnormal,anatomical system quality, abnormal,ansulate commissure decreased size, abnormal,ansulate commissure decreased size, abnormal,apoptotic process increased occurrence, abnormal"
        "MGI:88052"                      | 10        | "abnormal blood homeostasis,abnormal blood homeostasis,abnormal cholesterol homeostasis,abnormal cholesterol homeostasis,abnormal cholesterol homeostasis,abnormal circulating cholesterol level,abnormal circulating cholesterol level,abnormal circulating cholesterol level,abnormal circulating HDL cholesterol level,abnormal circulating HDL cholesterol level"
    }

}