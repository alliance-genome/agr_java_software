package org.alliancegenome.api

import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll


class QueryRankIntegrationSpec extends Specification {

    @Unroll
    def "When querying for #query with #filter, #betterResult comes before #worseResult"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def url = new URL("http://localhost:8080/api/search?limit=5000&offset=0&q=$encodedQuery$filter")
        def results = new JsonSlurper().parseText(url.text).results
        def betterResult = results.find { it.id == betterResultId }
        def worseResult = results.find { it.id == worseResultId }
        def betterResultPosition = results.findIndexValues() { it.id == betterResultId }?.first()
        def worseResultPosition = Integer.MAX_VALUE
        //if the "worse" result falls off the end of 5k results, for this test, that's a also a success
        if (worseResult != null) {
            worseResultPosition = results.findIndexValues() { it.id == worseResultId }?.first()
        }

        then:
        betterResult
        worseResult == null || betterResultPosition < worseResultPosition

        where:
        query                 | filter                               | betterResultId             | worseResultId             | issue
        "parkinson's disease" | "&category=gene&species=Danio+rerio" | "ZFIN:ZDB-GENE-050417-109" | "ZFIN:ZDB-GENE-040827-4"  | "AGR-461"
        "melanogaster kinase" | "&category=gene"                     | "FB:FBgn0028427"           | "RGD:1308199"             | "AGR-461"
        "maple bark"          | ""                                   | "DOID:8484"                | "FB:FBgn0031571"          | "AGR-461"
    }

    @Unroll
    def "When querying for #query in genes the symbol should start with #query"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def url = new URL("http://localhost:8080/api/search?category=gene&limit=50&offset=0&q=$encodedQuery")
        def results = new JsonSlurper().parseText(url.text).results
        def firstResultSymbol = results.first().get("symbol").toLowerCase()

        then:
        results //should be some results
        firstResultSymbol.startsWith(query)

        where:
        query << ["fgf", "pax"]

    }

    @Unroll
    def "All #query #n query genes should be on top when searching for #query"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def url = new URL("http://localhost:8080/api/search?category=gene&limit=50&offset=0&q=$encodedQuery")
        def names = (new JsonSlurper().parseText(url.text).results.take(n)*.symbol)*.toLowerCase()

        def autocompleteUrl = new URL("http://localhost:8080/api/search_autocomplete?q=$encodedQuery")
        def autoCompleteNames = (new JsonSlurper().parseText(autocompleteUrl.text).results.take(n)*.symbol)*.toLowerCase()

        then:
        names == Collections.nCopies(n,query)
        autoCompleteNames == Collections.nCopies(n,query)

        where:
        query   | n
        "egf"   | 4
        "fgf8"  | 3
        "pax2"  | 3

    }

    @Unroll
    def "When querying genes for #query #id should come back as a result according to #issue"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def url = new URL("http://localhost:8080/api/search?category=gene&limit=50&offset=0&q=$encodedQuery")
        def results = new JsonSlurper().parseText(url.text).results
        def resultIds = results*.id

        then:
        results
        resultIds.contains(id)

        where:
        query   | id                            | issue
        "smad6" | "MGI:1336883"                 | "AGR-580"
        "smad6" | "ZFIN:ZDB-GENE-050419-198"    | "AGR-580"
        "smad6" | "HGNC:6772"                   | "AGR-580"
        "smad6" | "ZFIN:ZDB-GENE-011015-1"      | "AGR-580"
        "smad6" | "RGD:1305069"                 | "AGR-580"
        "smad6" | "FB:FBgn0020493"              | "AGR-580"
        "smad6" | "WB:WBGene00006445"           | "AGR-580"

    }

    @Unroll
    def "When querying for #query first result name_key should be #nameKey"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def url = new URL("http://localhost:8080/api/search?limit=50&offset=0&q=$encodedQuery")
        def results = new JsonSlurper().parseText(url.text).results
        def firstResultNameKey = results.first().get("name_key")

        then:
        results //should be some results
        firstResultNameKey == nameKey

        where:
        query                              | nameKey
        "fgf8a (Dre)"                      | "fgf8a (Dre)"
        "Fgf8 (Mmu)"                       | "Fgf8 (Mmu)"
        "FGF8 (Hsa)"                       | "FGF8 (Hsa)"
        "pyr (Dme)"                        | "pyr (Dme)"
        "meg-2 (Cel)"                      | "meg-2 (Cel)"
        "Hps5 (Rno)"                       | "Hps5 (Rno)"
        "kinase activity"                  | "kinase activity"
        "kinase activi"                    | "kinase activity"
        "amyotrophic lateral sclerosis"    | "amyotrophic lateral sclerosis"

    }

}