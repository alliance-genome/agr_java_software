package org.alliancegenome.api


import spock.lang.Unroll

class DiseaseGeneTableSpec extends AbstractSpec {

    @Unroll
    def "Gene table on Disease page for #diseaseID"() {
        when:
        def disease = URLEncoder.encode(diseaseID, "UTF-8")
        def result = getApiResult("/api/disease/$disease/genes")
        def results = result.results

        then:
        results //should be some results
        size < result.total
        id == results[0].disease.id
        diseaseName == results[0].disease.name
        firstSpecies == results[0].gene.species.name
        firstGeneID == results[0].gene.symbol
        where:
        diseaseID   | id          | diseaseName                    | size | firstSpecies   | firstGeneID
        "DOID:1838" | "DOID:1838" | "Menkes disease"               | 20   | "Homo sapiens" | "ATP7A"
        "DOID:9952" | "DOID:9953" | "B- and T-cell mixed leukemia" | 60   | "Homo sapiens" | "DOT1L"
    }

    @Unroll
    def "Gene table on Disease page for #diseaseID : filtering #filterQuery"() {
        when:
        def disease = URLEncoder.encode(diseaseID, "UTF-8")
        def result = getApiResult("/api/disease/$disease/genes?$filterQuery")
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

    @Unroll
    def "Model table on Disease page for #diseaseID"() {
        when:
        def disease = URLEncoder.encode(diseaseID, "UTF-8")
        def result = getApiResult("/api/disease/$disease/models")
        def results = result.results

        then:
        results //should be some results
        size < result.total
        id == results[0].id
        diseaseName == results[0].disease.name
        firstModelName == results[0].name
        firstSpecies == results[0].species.name
        where:
        diseaseID   | id            | diseaseName            | size | firstSpecies   | firstModelName
        "DOID:1838" | "MGI:3618244" | "Menkes disease"       | 15   | "Mus musculus" | "Atp7a<sup>Mo-Tohm</sup>/Atp7a<sup>+</sup> [background:] B6.Cg-Atp7a<sup>Mo-Tohm</sup>"
        "DOID:62"   | "MGI:2176568" | "aortic valve disease" | 15   | "Mus musculus" | "Egfr<sup>wa2</sup>/Egfr<sup>wa2</sup> [background:] STOCK Egfr<sup>wa2</sup>"
    }

    @Unroll
    def "Model table on Disease page for #diseaseID : filtering #filterQuery"() {
        when:
        def disease = URLEncoder.encode(diseaseID, "UTF-8")
        def result = getApiResult("/api/disease/$disease/models?$filterQuery")
        def results = result.results

        then:
        results //should be some results
        size < result.total
        id == results[0].disease.id
        diseaseName == results[0].disease.name
        firstSpecies == results[0].species.name
        firstModelName == results[0].name
        where:
        filterQuery                    | diseaseID   | id          | diseaseName      | size | firstSpecies   | firstModelName
        "filter.modelName=v"           | "DOID:1838" | "DOID:1838" | "Menkes disease" | 6    | "Mus musculus" | "Atp7a<sup>Mo-br</sup>/? [background:] involves: C57BL"
        "filter.species=Danio%20rerio" | "DOID:1838" | "DOID:1838" | "Menkes disease" | 3    | "Danio rerio"  | "atp7a<sup>gw71/gw71</sup>"
        "filter.disease=m"             | "DOID:896"  | "DOID:1838" | "Menkes disease" | 20   | "Mus musculus" | "Atp7a<sup>Mo-Tohm</sup>/Atp7a<sup>+</sup> [background:] B6.Cg-Atp7a<sup>Mo-Tohm</sup>"
    }


    @Unroll
    def "Allele table on Disease page for #diseaseID"() {
        when:
        def disease = URLEncoder.encode(diseaseID, "UTF-8")
        def result = getApiResult("/api/disease/$disease/alleles")
        def results = result.results

        then:
        results //should be some results
        size < result.total
        id == results[0].disease.id
        diseaseName == results[0].disease.name
        firstSpecies == results[0].gene.species.name
        firstGeneID == results[0].gene.symbol
        alleleID == results[0].allele.id
        alleleName == results[0].allele.symbol
        where:
        diseaseID   | id          | diseaseName                  | size | firstSpecies   | firstGeneID | alleleID      | alleleName
        "DOID:1838" | "DOID:1838" | "Menkes disease"             | 8    | "Mus musculus" | "Atp7a"     | "MGI:1856098" | "Atp7a<sup>Mo-br</sup>"
        "DOID:9952" | "DOID:9952" | "acute lymphocytic leukemia" | 4    | "Mus musculus" | "Ezh2"      | "MGI:3823218" | "Ezh2<sup>tm2.1Sho</sup>"
    }

    @Unroll
    def "Allele table on Disease page for #diseaseID : filtering #filterQuery"() {
        when:
        def disease = URLEncoder.encode(diseaseID, "UTF-8")
        def result = getApiResult("/api/disease/$disease/alleles?$filterQuery")
        def results = result.results

        then:
        results //should be some results
        size < result.total
        id == results[0].disease.id
        diseaseName == results[0].disease.name
        firstSpecies == results[0].gene.species.name
        firstGeneSymbol == results[0].gene.symbol
        alleleID == results[0].allele.id
        alleleName == results[0].allele.symbol
        where:
        filterQuery                    | diseaseID   | id          | diseaseName      | size | firstSpecies   | firstGeneSymbol | alleleID                | alleleName
        "filter.geneName=l"            | "DOID:1838" | "DOID:1838" | "Menkes disease" | 6    | "Mus musculus" | "Lox"           | "MGI:2657016"           | "Lox<sup>tm1Ikh</sup>"
        "filter.alleleName=tm1"        | "DOID:1838" | "DOID:1838" | "Menkes disease" | 6    | "Mus musculus" | "Lox"           | "MGI:2657016"           | "Lox<sup>tm1Ikh</sup>"
        "filter.species=Danio%20rerio" | "DOID:1838" | "DOID:1838" | "Menkes disease" | 3    | "Danio rerio"  | "atp7a"         | "ZFIN:ZDB-ALT-090212-1" | "gw71"
        "filter.disease=m"             | "DOID:896"  | "DOID:1838" | "Menkes disease" | 20   | "Mus musculus" | "Atp7a"         | "MGI:1856098"           | "Atp7a<sup>Mo-br</sup>"
    }

}