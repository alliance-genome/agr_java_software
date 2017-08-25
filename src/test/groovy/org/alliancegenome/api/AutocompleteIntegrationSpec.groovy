import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll


class AutocompleteIntegrationSpec extends Specification {

    @Unroll
    def "an autocomplete query for #query should return results that start with #query"() {
        when:
        //todo: need to set the base search url in a nicer way
        def url = new URL("http://localhost:8080/api/search_autocomplete?q=$query")
        def results = new JsonSlurper().parseText(url.text).results
        def firstResult = results.first()

        then:
        results
        firstResult
        firstResult.get("name").toLowerCase().startsWith(query.toLowerCase())

        where:
        query << ["fgf","pax"]

    }

}