package groovy.org.alliancegenome.api

import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll


class PhenotypeAnnotationIntegrationSpec extends Specification {

    @Unroll
    def "Check that there are phenotype annotations for #query"() {
        when:
        def url = new URL("http://localhost:8080/api/gene/$query/phenotypes?limit=1000&")
        def results = new JsonSlurper().parseText(url.text).results

        then:
        results.size() > 10

        where:
        query << ["ZFIN:ZDB-GENE-990415-72","MGI:2443198"]

    }

    @Unroll
    def "Check different query parameters #query for phenotype annotation endpoint"() {
        when:
        def url = new URL("http://localhost:8080/api/gene/$gene/phenotypes?limit=1000&$query")
        def results = new JsonSlurper().parseText(url.text).results

        then:
        results.size() > resultSizeLowerLimit
        results.size() < resultSizeUpperLimit

        where:
        gene                      | query                          | resultSizeLowerLimit| resultSizeUpperLimit
        "ZFIN:ZDB-GENE-990415-72" | "termName=otic"               | 10                  | 20
        "ZFIN:ZDB-GENE-990415-72" | "geneticEntity=ti"             | 100                 | 150
        "ZFIN:ZDB-GENE-990415-72" | "reference=PMID"               | 100                 | 300
        "ZFIN:ZDB-GENE-990415-72" | "reference=ZFIN"               | 10                  | 30
        "ZFIN:ZDB-GENE-990415-72" | "reference=1466"               | 35                  | 60
        "MGI:2443198"             | "termName="                   | 30                  | 50
    }

    @Unroll
    def "Verify that the downloads endpoint has results"() {
        when:
        def url = new URL("http://localhost:8080/api/gene/$gene/phenotypes/download")
        def results = url.text.split('\n')

        then:
        results.size() > 100

        where:
        gene << ["ZFIN:ZDB-GENE-990415-72", "MGI:2443198"]
    }

}