package org.alliancegenome.api.tests.integration

import org.alliancegenome.api.tests.integration.ApiTester
import spock.lang.Specification
import spock.lang.Unroll

class AutocompleteIntegrationSpec extends Specification {

    @Unroll
    def "an autocomplete query for #query should return results that start with #query"() {
        when:
        //todo: need to set the base search url in a nicer way
        def results = ApiTester.getApiResults("/api/search_autocomplete?q=$query")
        def firstResult = results.first()

        then:
        results
        firstResult
        firstResult.get("name").toLowerCase().startsWith(query.toLowerCase())

        where:
        query << ["fgf","pax","ben"]

    }

    @Unroll
    def "selecting #category category should only return results from #category"() {
        when:
        def query = "f"
        //todo: need to set the base search url in a nicer way
        def results = ApiTester.getApiResults("/api/search_autocomplete?q=$query&category=$category")

        def categories = results*.category.unique()

        then:
        results
        categories
        categories.size() == 1
        categories == [category]

        where:
        category << ["gene", "disease", "go"]

    }

    @Unroll
    def "an autocomplete for #query should return #category results first"() {
        when:
        //todo: need to set the base search url in a nicer way
        def results = ApiTester.getApiResults("/api/search_autocomplete?q=$query")

        then:
        results
        results.size() > 3
        results.get(0).category == category
        results.get(1).category == category
        results.get(2).category == category

        where:
        category | query
        "gene"   | "pax"
        "gene"   | "fgf"
        "gene"   | "bmp"
    }

}