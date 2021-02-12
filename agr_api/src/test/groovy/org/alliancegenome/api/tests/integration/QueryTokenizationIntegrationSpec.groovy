package org.alliancegenome.api.tests.integration

import org.alliancegenome.api.service.SearchService
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import org.alliancegenome.api.tests.integration.ApiTester

class QueryTokenizationIntegrationSpec extends Specification {

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
        "Alzheimer's disease"               | ["Alzheimer's", "disease"]
        "ZDB-GENE-123456-123"               | ["ZDB-GENE-123456-123"]
        "\"NC_000083.6\\:g.75273979T>A\" MGI\\:5316784" | ["NC_000083.6:g.75273979T>A", "MGI:5316784"]

    }

    @Unroll
    def "#query should have #missing missing tokens for #resultId"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def results = ApiTester.getApiResults("/api/search?limit=5000&offset=0&q=$encodedQuery$filter")
        def result = results.find() { it.id == resultId }

        then:
        results
        results.size > 0
        result
        result.missingTerms == missing || (!result.missingTerms && !missing)

        where:
        filter             | query                                   | resultId                  | missing
        "&category=gene"   | "fgf8a pax2a"                           | "ZFIN:ZDB-GENE-990415-72" | ["pax2a"]
        "&category=gene"   | "fgf8a pax2a"                           | "ZFIN:ZDB-GENE-990415-8"  | ["fgf8a"]
        "&category=gene"   | "fgf8a ZFIN:ZDB-GENE-990415-72"         | "ZFIN:ZDB-GENE-990415-72" | []
        "&category=gene"   | "alzheimer's disease psen1"             | "HGNC:9508"               | []
        "&category=gene"   | "ZDB-GENE-001103-1 watermellon"         | "ZFIN:ZDB-GENE-001103-1"  | ["watermellon"]
        "&category=gene"   | "Two pore calcium channel protein 1"    | "HGNC:18182"              | []
        "&category=allele" | "NC_000083.6:g.75273979T>A MGI:5316784" | "MGI:5316784"             | []

    }


}
