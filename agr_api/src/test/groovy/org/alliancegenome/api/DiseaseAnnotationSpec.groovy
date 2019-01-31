package org.alliancegenome.api

import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll

class DiseaseAnnotationSpec extends Specification {

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
        crossRefs == disease.crossReferences.keySet().toList()[0]
        crossRefsOther < disease.crossReferences[crossRefs].size()
        crossRefOtherName == disease.crossReferences[crossRefs][0].name
        disease.crossReferences[crossRefs][0].url.startsWith(crossRefOtherUrl)
        disease.url.contains(doUrl)
        sources == disease.sources.size()
        where:
        query       | id          | name                         | parents | children | doUrl             | sources | crossRefs | crossRefsOther | crossRefOtherName | crossRefOtherUrl | definition                 | defLink
        "DOID:9952" | "DOID:9952" | "acute lymphocytic leukemia" | 1       | 4        | "disease-ontology"| 6       | "other"   | 10             | "NCI:C3167"       | "https://ncit.n" | "A lymphoblastic leukemia" | "http://www.cancer.gov/dictionary?CdrID=46332"

    }

    @Unroll
    def "Disease page - Annotations for #doid"() {
        when:
        def encodedQuery = URLEncoder.encode(doid, "UTF-8")
        def url = new URL("http://localhost:8080/api/disease/$encodedQuery/associations?limit=50")
        def retObj = new JsonSlurper().parseText(url.text)
        def results = retObj.results
        def ezha = results.find{it.gene.symbol == 'Ezh2' && it.allele}

        then:
        results //should be some results
        totalResults == retObj.total
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
        doid       | totalResults | returned | firstGene       | geneSymbol | crossRef        | geneticEntityType | evCode | disease                      | alleleSymbol             | alleleUrl                                     | doID        | doURL                                            | species
        "DOID:9952"| 66           | 50       | "FB:FBgn0265598"| "Bx"       | "PMID:22431509" | "allele"          | "TAS"  | "acute lymphocytic leukemia" | "Ezh2<sup>tm2.1Sho</sup>"| "http://www.informatics.jax.org/allele/MGI:3823218"|  "DOID:9952"| "http://www.disease-ontology.org/?id=DOID:9952"  | "Mus musculus"

    }
}