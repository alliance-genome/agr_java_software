package org.alliancegenome.api

import org.alliancegenome.api.service.SearchService
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class QueryTokenizationUnitSpec extends Specification {

    @Shared SearchService searchService

    def setup() {
        searchService = new SearchService()
    }

    @Unroll
    def "'#query' should have #tokens"() {
        when:
        def foundTokens = searchService.tokenizeQuery(query)

        then:
        foundTokens
        foundTokens.sort() == tokens.sort()

        where:
        query                               | tokens
        "fgf8a"                             | ["fgf8a"]
        "fgf8a pax2a"                       | ["fgf8a","pax2a"]
        "DOID:12345"                        | ["DOID:12345"]
        "DOID\\:12345"                      | ["DOID:12345"]
        "DOID:12345 foo bar"                | ["DOID:12345", "foo", "bar"]
        "pax* AND danio NOT \"paired box\"" | ["pax*", "danio", "paired box"] // eventually just pax* and danio?
        "ORDO:324569"                       | ["ORDO:324569"]

    }

}
