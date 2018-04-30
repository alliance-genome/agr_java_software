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
        def url = new URL("http://localhost:8080/api/search?limit=5000&offset=0&q=$encodedQuery$filter")
        def results = new JsonSlurper().parseText(url.text).results

        then:
        results
        results.size > 0

        where:
        filter              | query                                              | issue
        "&category=gene"    | "FBgn0086442"                                      | "AGR-525"
        "&category=gene"    | "FB:FBgn0086442"                                   | "AGR-525"
        "&category=gene"    | "ZDB-GENE-001120-2"                                | "AGR-525"
        "&category=gene"    | "ZFIN:ZDB-GENE-001120-2"                           | "AGR-525"
        "&category=gene"    | "WBGene00000244"                                   | "AGR-525"
        "&category=gene"    | "WB:WBGene00000244"                                | "AGR-525"
        "&category=allele"  | "MGI:5752578"                                      | "AGR-525"

    }


    @Unroll
    def "#query should match #id according to issue #issue"() {
        when:
        def encodedQuery = URLEncoder.encode("$query AND $id", "UTF-8")
        //todo: need to set the base search url in a nicer way
        def url = new URL("http://localhost:8080/api/search?limit=10&offset=0&q=$encodedQuery")
        def results = new JsonSlurper().parseText(url.text).results

        then:
        results
        results.size > 0

        where:
        issue      | id                | query
        //gene
        "AGR-934"  | "ZFIN:ZDB-GENE-990415-72" | "ENSEMBL:ENSDARG00000003399"
        "AGR-934"  | "ZFIN:ZDB-GENE-990415-72" | "ENSDARG00000003399"

        //disease
        "AGR-865"  | "DOID:0110047"     | "ICD10CM:G30"
        "AGR-865"  | "DOID:0110047"     | "G30"
        "AGR-865"  | "DOID:0110047"     | "OMIM:611154"
        "AGR-865"  | "DOID:0110047"     | "611154"

        //alleles
        "AGR-865"  | "ZFIN:ZDB-ALT-101119-1"     | "x15"
        "AGR-865"  | "MGI:1856016"     | "Edar<sup>dl</sup>"
        "AGR-865"  | "MGI:1856016"     | "Edar"
//Would require loading a text version of the allele names with < > instead of <sup> tags
//        "AGR-865"  | "MGI:1856016"     | "Edar<dl>"
        "AGR-865"  | "MGI:1856016"     | "Edardl"
        "AGR-865"  | "MGI:1855960"     | "Tyrp1<sup>b</sup>"
        "AGR-865"  | "MGI:3923395"     | "Cd99<sup>Gt(Ayu21-B6T44)Imeg</sup>"
        "AGR-865"  | "MGI:5752578"     | "Ace2<sup>em#Yngh</sup>"
        "AGR-865"  | "MGI:5000472"     | "Gt(ROSA)26Sor<sup>tm1(Pik3ca*H1047R)Egan</sup>"
        "AGR-865"  | "WBVar:WBVar00143949"     | "e1370"
        "AGR-865"  | "WBVar:WBVar00143949"     | "WBVar00143949"
//This is another one where the text from the testing document isn't in the record,
//e1370 is the symbol, daf-2 is the gene, if daf-2(e1370) is going to match, it probably
//needs to at least be a synonym
        "AGR-865"  | "WBVar:WBVar00143949"     | "daf-2(e1370)"
        "AGR-865"  | "RGD:728298"      | "Brca1<i><sup>m1Uwm</sup></i>"
        "AGR-865"  | "RGD:1600311"     | "Rab38<sup>ru</sup>"
        "AGR-865"  | "RGD:7241044"     | "Lrrk1<sup>em1Sage</sup>"
        "AGR-865"  | "RGD:2311687"     | "Fam227a<sup>Tn(sb-T2/Bart3)2.333Mcwi</sup>"
        "AGR-865"  | "RGD:2311687"     | "Fam227aTn(sb-T2/Bart3)2.333Mcwi"
//These two come from the testing document, but I don't believe that we have them
//It's also possible that they're need to be broken up into several queries
//        "AGR-865"  | "RGD:2292447"     | "RGD1564304Tn(sb-T2/Bart3)2.201Mcwi"
//        "AGR-865"  | "RGD:2292447"     | "RGD1564304<sup>Tn(sb-T2/Bart3)2.201Mcwi</sup>Tgrdw"
        "AGR-865"  | "RGD:12879860"     | "Tg<sup>rdw</sup>"
        "AGR-865"  | "MGI:5502315"     | "Rradtm1.1(KOMP)Vlcg"
        "AGR-865"  | "FB:FBal0151567"  | "rut[EP399]"
    }
}
