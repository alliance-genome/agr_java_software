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
        query                 | issue
        "FBgn0086442"         | "AGR-525"
        "ZDB-GENE-001120-2"   | "AGR-525"
        "WBGene00000244"      | "AGR-525"

    }

}