package org.alliancegenome.api


import spock.lang.Unroll

class GenePageSpec extends AbstractSpec {

    @Unroll
    def "AGM table on Gene page for #geneID"() {
        when:
        def gene = URLEncoder.encode(geneID, "UTF-8")
        def result = getApiResult("/api/gene/$gene/models")
        def results = result.results

        then:
        results //should be some results
        size <= result.total
        id == results[0].diseases[0].id
        diseaseName == results[0].diseases[0].name
        // number of disease terms for given model
        diseaseSize == results[0].diseases.size
        modelID == results[0].id
        modelName == results[0].name
        where:
        geneID                     | modelID                      | id             | diseaseName                    | size | diseaseSize | modelName
        "MGI:107718"               | "MGI:5296754"                | "DOID:0110599" | "primary ciliary dyskinesia 3" | 12   | 3           | "Dnah5<sup>b2b002Clo</sup>/Dnah5<sup>b2b002Clo</sup> [background:] C57BL/6J-Dnah5<sup>b2b002Clo</sup>"
        "ZFIN:ZDB-GENE-040625-147" | "ZFIN:ZDB-FISH-150901-17337" | "DOID:1339"    | "Diamond-Blackfan anemia"      | 3    | 1           | "WT + MO1-rpl11"
    }

    @Unroll
    def "AGM table on Gene page for #geneID : filtering #filterQuery"() {
        when:
        def gene = URLEncoder.encode(geneID, "UTF-8")
        def result = getApiResult("/api/gene/$gene/genes?$filterQuery")
        def results = result.results

        then:
        results //should be some results
        size < result.total
        id == results[0].disease.id
        diseaseName == results[0].disease.name
        firstSpecies == results[0].gene.species.name
        firstGeneID == results[0].gene.symbol
        where:
        filterQuery                    | diseaseID   | id          | diseaseName       | size | firstSpecies   | firstGeneID
        "filter.geneName=l"            | "DOID:1838" | "DOID:1838" | "Menkes disease"  | 6    | "Homo sapiens" | "LOX"
        "filter.species=Danio%20rerio" | "DOID:1838" | "DOID:1838" | "Menkes disease"  | 3    | "Danio rerio"  | "atp7a"
        "filter.disease=m"             | "DOID:896"  | "DOID:2352" | "hemochromatosis" | 20   | "Homo sapiens" | "ALAS2"
    }

}