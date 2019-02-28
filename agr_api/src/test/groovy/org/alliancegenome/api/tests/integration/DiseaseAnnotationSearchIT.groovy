package org.alliancegenome.api

import groovy.json.JsonSlurper
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class DiseaseAnnotationSearchIT extends Specification {

    @Ignore("Not working until we get disease data on geneMap")
    @Unroll
    def "When querying for #query with #filter, #betterResult comes before #worseResult"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def url = new URL("http://localhost:8080/api/search?category=gene&limit=500&offset=0&q=$encodedQuery$filter")
        def results = new JsonSlurper().parseText(url.text).results
        def betterResult = results.find { it.id == betterResultId }
        def worseResult = results.find { it.id == worseResultId }
        def betterResultPosition = results.findIndexValues() { it.id == betterResultId }?.first()
        def worseResultPosition = results.findIndexValues() { it.id == worseResultId }?.first()

        then:
        betterResult
        worseResult
        betterResultPosition < worseResultPosition

        where:
        query                 | filter                 | betterResultId             | worseResultId             | issue
        "parkinson's disease" | "&species=Danio+rerio" | "ZFIN:ZDB-GENE-050417-109" | "ZFIN:ZDB-GENE-040827-4"  | "AGR-461"
// sadly, this one is a tougher nut to crack
//      "melanogaster kinase" | ""                     | "FB:FBgn0028427"                | "RGD:1308199"        | "AGR-461"
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
}