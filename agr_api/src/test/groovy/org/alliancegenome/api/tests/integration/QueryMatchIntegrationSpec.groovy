package org.alliancegenome.api

import org.alliancegenome.api.tests.integration.ApiTester
import spock.lang.Specification
import spock.lang.Unroll

class QueryMatchIntegrationSpec extends Specification {

    @Unroll
    def "#query should return some results"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def results = ApiTester.getApiResults("/api/search?limit=5000&offset=0&q=$encodedQuery$filter")

        then:
        results
        results.size > 0

        where:
        filter             | query                    | issue
        "&category=gene"   | "FBgn0086442"            | "AGR-525"
        "&category=gene"   | "FB:FBgn0086442"         | "AGR-525"
        "&category=gene"   | "ZDB-GENE-001120-2"      | "AGR-525"
        "&category=gene"   | "ZFIN:ZDB-GENE-001120-2" | "AGR-525"
        "&category=gene"   | "WBGene00000244"         | "AGR-525"
        "&category=gene"   | "WB:WBGene00000244"      | "AGR-525"
        "&category=allele" | "MGI:5752578"            | "AGR-525"

    }


    @Unroll
    def "#query should match #id according to issue #issue"() {
        when:
        def encodedQuery = URLEncoder.encode("$query AND $id", "UTF-8")
        //todo: need to set the base search url in a nicer way
        def results = ApiTester.getApiResults("/api/search?limit=10&offset=0&q=$encodedQuery")

        then:
        results
        results.size > 0

        where:
        issue      | id                        | query
        //gene
        "AGR-934"  | "ZFIN:ZDB-GENE-990415-72" | "ENSEMBL:ENSDARG00000003399"
        "AGR-934"  | "ZFIN:ZDB-GENE-990415-72" | "ENSDARG00000003399"
        "AGR-1048" | "MGI:99604"               | "Predicted to have biological regulation, chemoattractant activity, and signaling receptor binding"
        //this is from automatedGeneSynopsis, so it may change over time
        "AGR-1048" | "MGI:99604"               | "Predicted to have signaling receptor binding activity. Involved in several processes, including animal organ development; embryonic morphogenesis"

        //disease
        "AGR-865"  | "DOID:0110047"            | "ICD10CM:G30"
        "AGR-865"  | "DOID:0110047"            | "G30"
        "AGR-865"  | "DOID:0110047"            | "OMIM:611154"
        "AGR-865"  | "DOID:0110047"            | "611154"

        //alleles
        "AGR-865"  | "ZFIN:ZDB-ALT-101119-1"   | "x15"
        "AGR-865"  | "MGI:1856016"             | "Edar<sup>dl</sup>"
        "AGR-865"  | "MGI:1856016"             | "Edar"
        "AGR-865"  | "MGI:1856016"             | "Edar<dl>"
        "AGR-865"  | "MGI:1856016"             | "Edardl"
        "AGR-865"  | "MGI:1855960"             | "Tyrp1<sup>b</sup>"
        "AGR-865"  | "MGI:3923395"             | "Cd99<sup>Gt(Ayu21-B6T44)Imeg</sup>"
        "AGR-865"  | "MGI:5752578"             | "Ace2<sup>em#Yngh</sup>"
        "AGR-865"  | "MGI:5000472"             | "Gt(ROSA)26Sor<sup>tm1(Pik3ca*H1047R)Egan</sup>"
        "AGR-865"  | "WB:WBVar00143949"        | "e1370"
        "AGR-865"  | "WB:WBVar00143949"        | "WBVar00143949"
//This is another one where the text from the testing document isn't in the record,
//e1370 is the symbol, daf-2 is the gene, if daf-2(e1370) is going to match, it probably
//needs to at least be a synonym
//        "AGR-865"  | "WBVar:WBVar00143949"     | "daf-2(e1370)"
        "AGR-865"  | "RGD:728298"              | "Brca1<i><sup>m1Uwm</sup></i>"
        "AGR-865"  | "RGD:1600311"             | "Rab38<sup>ru</sup>"
        "AGR-865"  | "RGD:7241044"             | "Lrrk1<sup>em1Sage</sup>"
        "AGR-865"  | "RGD:2311687"             | "Fam227a<sup>Tn(sb-T2/Bart3)2.333Mcwi</sup>"
        "AGR-865"  | "RGD:2311687"             | "Fam227aTn(sb-T2/Bart3)2.333Mcwi"

        "AGR-865"  | "RGD:12879860"            | "Tg<sup>rdw</sup>"
        "AGR-865"  | "MGI:5502315"             | "Rradtm1.1(KOMP)Vlcg"
        "AGR-865"  | "FB:FBal0151567"          | "rut[EP399]"
        "AGR-508"  | "FB:FBal0036007"          | "en11"

        "AGR-2006" | "MGI:3524957"             | "Tg(APPswe,PSEN1dE9)85Dbo"
        "AGR-2006" | "MGI:3618599"             | "Tg(PSEN1dE9)S9Dbo"
        "AGR-2006" | "MGI:3693208"             | "Tg(APPSwFlLon,PSEN1*M146L*L286V)6799Vas"
        "AGR-2006" | "ZFIN:ZDB-ALT-140814-9"   | "zou011Tg"

        //variant hgvs names for alleles
        "AGR-1899" | "MGI:5316784"               | "NC_000083.6:g.75273979T>A"
        "AGR-1899" | "ZFIN:ZDB-ALT-161003-18649" | "NC_007116.7:g.23258951T>A"
        "AGR-1899" | "RGD:13209000"              | "NC_005118.4:g.55252024_55252027del"
        "AGR-1899" | "FB:FBal0000019"            | "NT_033777.3:g.7087759G>A"


        //diseasesViaExperiment found by allele and gene names
        "AGR-866"  | "DOID:11726"              | "tm1502"
        "AGR-866"  | "DOID:0050692"            | "mi289a"
        "AGR-866"  | "DOID:5327"               | "IL6"
        "AGR-866"  | "DOID:9352"               | "Lep"

        //strict orthology
        "AGR-755"  | "ZFIN:ZDB-GENE-010323-11" | "ena"
        "AGR-755"  | "ZFIN:ZDB-GENE-010323-11" | "ENAH"
        "AGR-755"  | "ZFIN:ZDB-GENE-010323-11" | "en" //confirm starts-with match too

        //secondaryId
        "AGR-1437" | "MGI:109583"              | "MGI:1917411"

        //expression stages
        "AGR-1956" | "ZFIN:ZDB-GENE-980605-16"   | "Segmentation:14-19 somites"
        "AGR-1956" | "ZFIN:ZDB-GENE-040426-2238" | "Gastrula:50%-epiboly"
        "AGR-1956" | "ZFIN:ZDB-GENE-040426-2238" | "Gastrula"
        "AGR-1956" | "FB:FBgn0262417"            | "embryonic stage 13"
        "AGR-1956" | "FB:FBgn0261439"            | "day 17 of adulthood"
        "AGR-1956" | "WB:WBGene00077727"         | "Nematoda Life Stage"
        "AGR-1956" | "WB:WBGene00000604"         | "dauer larva Ce"
        "AGR-1956" | "MGI:97750"                 | "TS23"
        "AGR-1956" | "MGI:88537"                 | "TS18"

    }

    @Unroll
    def "#query should NOT match #id according to issue #issue"() {
        when:
        def encodedQuery = URLEncoder.encode("NOT $query AND $id", "UTF-8")
        //todo: need to set the base search url in a nicer way
        def results = ApiTester.getApiResults("/api/search?limit=10&offset=0&q=$encodedQuery")

        then:
        results
        results.size > 0
        results.get(0).id == id //expect that an id brings back the first match

        where:
        issue     | id                        | query
        //strict orthology
        "AGR-755" | "ZFIN:ZDB-GENE-010323-11" | "Y20F4.4"
        "AGR-755" | "ZFIN:ZDB-GENE-010323-11" | "EVL"
        "AGR-755" | "SGD:S000000004"          | "Hspa4l"
        "AGR-755" | "ZFIN:ZDB-GENE-121214-31" | "hspa1a"
        "AGR-755" | "FB:FBgn0001218"          | "hspa1a"
    }

}