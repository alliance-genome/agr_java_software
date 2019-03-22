package org.alliancegenome.api

import spock.lang.Unroll

class PhenotypeAnnotationIntegrationSpec extends AbstractSpec {

    @Unroll
    def "Check that there are phenotype annotations for #query"() {
        when:
        def results = getApiResults("/api/gene/$query/phenotypes?limit=1000&").results

        then:
        results.size() > 10

        where:
        query << ["ZFIN:ZDB-GENE-990415-72", "MGI:2443198"]

    }

    @Unroll
    def "Check different query parameters #query for phenotype annotation endpoint"() {
        when:
        def results = getApiResults("/api/gene/$gene/phenotypes?limit=1000&$query").results

        then:
        results.size() > resultSizeLowerLimit
        results.size() < resultSizeUpperLimit

        where:
        gene                      | query                     | resultSizeLowerLimit | resultSizeUpperLimit
        "ZFIN:ZDB-GENE-990415-72" | "filter.termName=otic"    | 10                   | 40
        "ZFIN:ZDB-GENE-990415-72" | "filter.geneticEntity=ti" | 100                  | 150
        "ZFIN:ZDB-GENE-990415-72" | "filter.reference=PMID"   | 100                  | 300
        "ZFIN:ZDB-GENE-990415-72" | "filter.reference=ZFIN"   | 10                   | 30
        "ZFIN:ZDB-GENE-990415-72" | "filter.reference=1466"   | 35                   | 60
        "MGI:2443198"             | "filter.termName="        | 30                   | 50
    }

    @Unroll
    def "Verify that the downloads endpoint has results"() {
        when:
        def result = getApiResultRaw("/api/gene/$gene/phenotypes/download")
        def results = result.split('\n')

        then:
        results.size() > 10

        where:
        gene << ["ZFIN:ZDB-GENE-990415-72", "MGI:2443198"]
    }

}