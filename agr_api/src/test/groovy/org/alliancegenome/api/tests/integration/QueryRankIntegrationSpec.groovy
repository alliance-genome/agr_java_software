package org.alliancegenome.api

import org.alliancegenome.api.tests.integration.ApiTester
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class QueryRankIntegrationSpec extends Specification {

    @Unroll
    @Ignore
    def "When querying for #query with #filter, #betterResult comes before #worseResult"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def results = ApiTester.getApiResults("/api/search?limit=5000&offset=0&q=$encodedQuery$filter")

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
    @Ignore
    def "When querying for #query in genes the symbol should start with #query"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def results = ApiTester.getApiResults("/api/search?category=gene&limit=50&offset=0&q=$encodedQuery")
        def firstResultSymbol = results.first().get("symbol").toLowerCase()

        then:
        results //should be some results
        firstResultSymbol.startsWith(query)

        where:
        query << ["fgf", "pax"]

    }

    @Unroll
    @Ignore
    def "All #query #n query genes should be on top when searching for #query"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def results = ApiTester.getApiResults("/api/search?category=gene&limit=50&offset=0&q=$encodedQuery")
        def names = (results.take(n)*.symbol)*.toLowerCase()

        def results2 = ApiTester.getApiResults("/api/search_autocomplete?q=$encodedQuery")
        def autoCompleteNames = (results2.take(n)*.symbol)*.toLowerCase()

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
    @Ignore
    def "When querying genes for #query #id should come back as a result according to #issue"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def results = ApiTester.getApiResults("/api/search?category=gene&limit=50&offset=0&q=$encodedQuery")

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
    @Ignore
    def "When querying for #query first result name_key should be #nameKey"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def results = ApiTester.getApiResults("/api/search?limit=50&offset=0&q=$encodedQuery")

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
        "human SLC11A1"                    | "SLC11A1 (Hsa)"
        "human wnt2b"                      | "WNT2B (Hsa)"
        "human wnt10b"                     | "WNT10B (Hsa)"
        "TP73 (Hsa)"                       | "TP73 (Hsa)"
        "MGI:97769"                        | "Prnp (Mmu)"
        "MGI:1891396"                      | "Kirrel (Mmu)"
        "MGI:98364"                        | "Sox2 (Mmu)"
        "MGI:109430"                       | "Icam5 (Mmu)"
        "SGD:S000006169"                   | "GAL4 (Sce)"
        "HGNC:11998"                       | "TP53 (Hse)"
        "FB:FBgn0003996"                   | "w (Dme)"

    }

    @Unroll
    def "when querying for #query #id #nameKey in category #category should be in the top #n results"() {
       when:
       def encodedQuery = URLEncoder.encode(query, "UTF-8")
       //todo: need to set the base search url in a nicer way

       def results = ApiTester.getApiResults("/api/search?limit=50&offset=0&category=$category&q=$encodedQuery")
       def positions = [:]
       results.eachWithIndex { result, i -> positions.put(result.id, i+1) }

       then:
       positions.get(id)
       positions.get(id) <= n

       where:
       query                            | id           | nameKey        | category | n
       "breast cancer"                  | "HGNC:1100"  | "BRCA1 (Hsa)"  | "gene"   | 2
       "breast cancer"                  | "HGNC:1101"  | "BRCA2 (Hsa)"  | "gene"   | 2
       "Huntington’s"                   | "HGNC:4851"  | "HTT (Hsa)"    | "gene"   | 7
       "familial adenomatous polyposis" | "HGNC:583"   | "APC (Hsa)"    | "gene"   | 3
       "Parkinson’s"                    | "HGNC:8607"   | "PRKN (Hsa)"  | "gene"   | 20
//       "Alzheimer’s"                    | "HGNC:620"    | "APP (Hsa)"   | "gene"   | 10 //failing at 27
//       "Alzheimer’s"                    | "HGNC:9508"   | "PSEN1 (Hsa)" | "gene"   | 10 //failing at 223 !
//       "Alzheimer’s"                    | "HGNC:9509"   | "PSEN2 (Hsa)" | "gene"   | 10 //failing at 21
       "Cystic fibrosis"                | "HGNC:1884"   | "CFTR"        | "gene"   | 1
    }

}
