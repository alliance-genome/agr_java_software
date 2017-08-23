import spock.lang.Specification
import spock.lang.Unroll
import groovy.json.JsonSlurper;


class QueryRankIntegrationSpec extends Specification {


    @Unroll
    def "When querying for #query with #filter, #betterResult comes before #worseResult"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        def url = new URL("http://localhost:8080/api/search?category=gene&limit=50&offset=0&q=$encodedQuery$filter")
        def results = new JsonSlurper().parseText(url.text).results
        def betterResultPosition = results.findIndexValues() { it.id == betterResult }.first()
        def worseResultPosition = results.findIndexValues() { it.id == worseResult }.first()

        then:
        betterResultPosition < worseResultPosition

        where:
        query                 | filter                 | betterResult                    | worseResult
        "parkinson's disease" | "&species=Danio+rerio" | "ZFIN:ZDB-GENE-050417-109"      | "ZFIN:ZDB-GENE-040827-4"
    }

}