package org.alliancegenome.api

import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll

class DiseaseAnnotationIntegrationSpec extends Specification {

    @Unroll
    def "Disease page for #query"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def url = new URL("http://localhost:8080/api/disease/$encodedQuery")
        def disease = new JsonSlurper().parseText(url.text)

        then:
        disease //should be some results
        id == disease.id
        name == disease.name
        disease.definition.startsWith(definition)
        defLink == disease.definitionLinks.first()
        parents == disease.parents.size()
        children == disease.children.size()
        disease.crossReferences.keySet().toList()[0]
        disease.crossReferences[crossRefs].size()
        disease.crossReferences[crossRefs][0].name
        sources == disease.sources.size()
        where:
        query       | id          | name                         | parents | children | doUrl              | sources | crossRefs | crossRefsOther | definition                 | defLink
        "DOID:9952" | "DOID:9952" | "acute lymphocytic leukemia" | 1       | 4        | "disease-ontology" | 6       | "other"   | 10             | "A lymphoblastic leukemia" | "http://www.cancer.gov/dictionary?CdrID=46332"

    }

    @Unroll
    def "Disease page - Annotations for #doid"() {
        when:
        def encodedQuery = URLEncoder.encode(doid, "UTF-8")
        def url = new URL("http://localhost:8080/api/disease/$encodedQuery/associations?limit=50")
        def retObj = new JsonSlurper().parseText(url.text)
        def results = retObj.results
        def ezha = results.find { it.gene.symbol == 'Ezh2' && it.allele }

        then:
        results //should be some results
        totalResults <= retObj.total
        returned == results.size()
        firstGene == results.first().gene.id
        //firstGeneUrl == results.first().gene.url
        geneSymbol == results.first().gene.symbol
        ezha
        alleleSymbol == ezha.allele.symbol
        alleleUrl == ezha.allele.url
        crossRef == ezha.publications[0].id
        geneticEntityType == ezha.geneticEntityType
        evCode == ezha.evidenceCodes[0].name
        disease == ezha.disease.name
        doID == ezha.disease.id
        doURL == ezha.disease.url
        species == ezha.gene.species.name
        where:
        doid        | totalResults | returned | firstGene    | geneSymbol | crossRef        | geneticEntityType | evCode | disease                      | alleleSymbol              | alleleUrl                                           | doID        | doURL                                           | species
        "DOID:9952" | 66           | 50       | "HGNC:24948" | "DOT1L"    | "PMID:22431509" | "allele"          | "TAS"  | "acute lymphocytic leukemia" | "Ezh2<sup>tm2.1Sho</sup>" | "http://www.informatics.jax.org/allele/MGI:3823218" | "DOID:9952" | "http://www.disease-ontology.org/?id=DOID:9952" | "Mus musculus"

    }

    @Unroll
    def "Disease page - Annotations for #sortBy - Sorting"() {
        when:
        def url = new URL("http://localhost:8080/api/disease/DOID:9952/associations?limit=15&sortBy=$sortBy")
        def retObj = new JsonSlurper().parseText(url.text)
        def results = retObj.results
        def symbols = results.gene.symbol.findAll { it }
        def species = results.gene.species.name.findAll { it }
        def disease = results.disease.name.findAll { it }

        then:
        results
        symbols.join(",") == geneSymbolList
        species.join(",") == speciesList
        disease.join(",") == diseaseList

        where:
        sortBy     | geneSymbolList                                                                        | speciesList                                                                                                                                                                                                              | diseaseList
        "geneName" | "DOT1L,KMT2A,Cntn2,Ezh2,Ezh2,Ezh2,Kmt2a,Kmt2a,Lmo2,Notch3,Pten,Pten,Zeb2,ces-2,ces-2" | "Homo sapiens,Homo sapiens,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Caenorhabditis elegans,Caenorhabditis elegans" | "B- and T-cell mixed leukemia,B- and T-cell mixed leukemia,T-cell adult acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,T-cell adult acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia"
        "species"  | "DOT1L,KMT2A,Cntn2,Ezh2,Ezh2,Ezh2,Kmt2a,Kmt2a,Lmo2,Notch3,Pten,Pten,Zeb2,ces-2,ces-2" | "Homo sapiens,Homo sapiens,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Caenorhabditis elegans,Caenorhabditis elegans" | "B- and T-cell mixed leukemia,B- and T-cell mixed leukemia,T-cell adult acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,T-cell adult acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia"
        "disease"  | "DOT1L,KMT2A,Ezh2,Ezh2,Ezh2,Kmt2a,Kmt2a,Lmo2,Notch3,Pten,Pten,Cntn2,Zeb2,ces-2,ces-2" | "Homo sapiens,Homo sapiens,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Mus musculus,Caenorhabditis elegans,Caenorhabditis elegans" | "B- and T-cell mixed leukemia,B- and T-cell mixed leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,T-cell adult acute lymphocytic leukemia,T-cell adult acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia"

    }

    @Unroll
    def "Disease page - Annotations for #limit - limit"() {
        when:
        def url = new URL("http://localhost:8080/api/disease/DOID:9952/associations?limit=$limit")
        def retObj = new JsonSlurper().parseText(url.text)
        def results = retObj.results

        then:
        results
        results.size() >= resultSize

        where:
        limit | resultSize
        5     | 5
        10    | 10
        50    | 50
        500   | 66

    }

    @Unroll
    def "Disease page - Annotation Filtering for #geneSymbolQuery "() {
        when:
        def url = new URL("http://localhost:8080/api/disease/DOID:9952/associations?limit=50&filter.geneName=$geneSymbolQuery")
        def retObj = new JsonSlurper().parseText(url.text)
        def results = retObj.results
        def symbols = results.gene.symbol.findAll { it }

        then:
        results
        resultSize == results.size()
        geneSymbolList == symbols.join(",")

        where:
        geneSymbolQuery | resultSize | geneSymbolList
        "ot"            | 13         | "DOT1L,Notch3,NOTCH3,Dot1l,Notch3,Dot1l,dot1l,notch3,dot-1.1,dot-1.2,dot-1.4,dot-1.5,DOT1"
        "2a"            | 10         | "KMT2A,Kmt2a,Kmt2a,KMT2A,Kmt2a,Kmt2a,Kmt2a,kmt2a,kmt2a,zeb2a"
        "r"             | 2          | "trx,trx"
    }

    @Unroll
    def "Gene page - Annotation via Experiment #geneID "() {
        when:
        def url = new URL("http://localhost:8080/api/gene/$geneID/diseases-by-experiment?limit=5")
        def retObj = new JsonSlurper().parseText(url.text)
        def results = retObj.results

        then:
        results
        retObj.total == resultSize
        results[0].disease.name == diseaseName
        results[0].disease.id == diseaseID
        results[0].disease.url == diseaseURl
        results[0].allele.symbol == alleleSymbol
        results[0].allele.url == alleleUrl

        where:
        geneID                   | resultSize | diseaseName                  | diseaseID    | diseaseURl                                       | alleleSymbol            | alleleUrl
        "MGI:109583"             | 50         | "acute lymphocytic leukemia" | "DOID:9952"  | "http://www.disease-ontology.org/?id=DOID:9952"  | "Pten<sup>tm1Hwu</sup>" | "http://www.informatics.jax.org/allele/MGI:2156086"
        "ZFIN:ZDB-GENE-990415-8" | 1          | "coloboma"                   | "DOID:12270" | "http://www.disease-ontology.org/?id=DOID:12270" | "tu29a"                 | "https://zfin.org/ZDB-ALT-980203-1248"
    }

    @Unroll
    def "Gene page - Annotation via Orthology #geneID "() {
        when:
        def url = new URL("http://localhost:8080/api/gene/$geneID/diseases-via-orthology?limit=5")
        def retObj = new JsonSlurper().parseText(url.text)
        def results = retObj.results

        then:
        results
        retObj.total == resultSize
        results[0].disease.name == diseaseName
        results[0].disease.id == diseaseID
        results[0].disease.url == diseaseURl
        results[0].orthologyGene.id == orthoGene
        results[0].source.name == source

        where:
        geneID                   | resultSize | diseaseName                            | diseaseID      | source     | orthoGene   | diseaseURl
        "MGI:109583"             | 31         | "angiomyolipoma"                       | "DOID:3314"    | "Alliance" | "HGNC:9588" | "http://www.disease-ontology.org/?id=DOID:3314"
        "ZFIN:ZDB-GENE-990415-8" | 5          | "focal segmental glomerulosclerosis 7" | "DOID:0111132" | "Alliance" | "HGNC:8616" | "http://www.disease-ontology.org/?id=DOID:0111132"
    }

    @Unroll
    def "Disease page - Annotations including child terms #doID "() {
        when:
        def url = new URL("http://localhost:8080/api/disease/$doID/associations")
        def retObj = new JsonSlurper().parseText(url.text)
        def results = retObj.results

        then:
        results
        //retObj.total == resultSize

        where:
        doID           | resultSize
        "DOID:1070"    | 172
        "DOID:1067"    | 365
        "DOID:1686"    | 858
        "DOID:0111077" | 858
    }

}