package org.alliancegenome.api

import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll


class QueryMatchIntegrationSpec extends Specification {

    @Unroll
    def "#query should return some results"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def url = new URL("http://localhost:8080/api/search?limit=5000&offset=0&q=$encodedQuery")
        def results = new JsonSlurper().parseText(url.text).results

        then:
        results
        results.size > 0

        where:
        query                                              | issue
        "FBgn0086442"                                      | "AGR-525"
        "FB:FBgn0086442"                                   | "AGR-525"
        "ZDB-GENE-001120-2"                                | "AGR-525"
        "ZFIN:ZDB-GENE-001120-2"                           | "AGR-525"
        "WBGene00000244"                                   | "AGR-525"
        "WB:WBGene00000244"                                | "AGR-525"

        "ENSEMBL:ENSDARG00000003399 AND ZFIN:ZDB-GENE-990415-72" | "AGR-934"

        "DOID:0110047"                                     | "AGR-604"
        "ICD10CM:G30 AND DOID:0110047"                     | "AGR-604"
        "G30 AND DOID:0110047"                             | "AGR-604"
        "OMIM:611154 AND DOID:0110047"                     | "AGR-604"
        "611154 AND DOID:0110047"                          | "AGR-604"



    }

}