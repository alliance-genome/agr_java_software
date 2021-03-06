import org.alliancegenome.api.tests.integration.ApiTester
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class DiseaseAnnotationIntegrationSpec extends Specification {

    @Unroll
    def "Disease Download File: #species for disease #query"() {
        when:
        def doiID = URLEncoder.encode(query, "UTF-8")
        def results = ApiTester.getApiResultRaw("/api/disease/annotation/download?diseaseID=$doiID&species=$species")

        then:
        results //should be some results
        results.split("\n").length > numberOfLines

        where:
        query       | species | numberOfLines
        "DOID:1838" | "mus"   | 69
        "DOID:1838" | "danio" | 23
        "DOID:1838" | "eleg"  | 10
        "DOID:4"    | "eleg"  | 1000

    }

    @Unroll
    def "Disease Page: Gene table: #speciesName for disease #doiID and IGI evidence Code"() {
        when:
        def species = URLEncoder.encode(speciesName, "UTF-8")
        def results = ApiTester.getApiResult("/api/disease/$doiID/genes?filter.species=$species&filter.evidenceCode=igi")

        then:
        results //should be some results
        results.total > numberOfLines

        where:
        doiID      | speciesName                | numberOfLines
        "DOID:162" | "Saccharomyces cerevisiae" | 70
    }

    @Unroll
    def "Disease page: allele section sort by allele for #query"() {
        when:
        def doiID = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def results = ApiTester.getApiResults("/api/disease/$doiID/alleles?sortBy=allele")
        def alleleNames = results.allele.symbolText
        def alleleNamesSorted = alleleNames.clone().sort { a, b -> a.compareToIgnoreCase b }

        then:
        results //should be some results
        id == results[0].disease.id
        name == results[0].disease.name
        numOfRecords <= results.size()
        alleleNames.equals(alleleNamesSorted)

        where:
        query       | id          | name             | numOfRecords
        "DOID:1838" | "DOID:1838" | "Menkes disease" | 10

    }

    @Unroll
    def "Disease page: allele section sort by disease for #query"() {
        when:
        def doiID = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def results = ApiTester.getApiResults("/api/disease/$doiID/alleles?sortBy=disease&limit=100")
        def diseaseNames = results.disease.name
        def diseaseNamesSorted = diseaseNames.clone().sort { a, b -> a.compareToIgnoreCase b }

        then:
        results //should be some results
        numOfRecords <= results.size()
        diseaseNames.equals(diseaseNamesSorted)

        where:
        query       | numOfRecords
        "DOID:2531" | 70

    }


    @Unroll
    def "Disease page for #query"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def disease = ApiTester.getApiResult("/api/disease/$encodedQuery")

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
        query       | id          | name                           | parents | children | doUrl              | sources | crossRefs | crossRefsOther | definition                                                                   | defLink
        "DOID:9952" | "DOID:9952" | "acute lymphoblastic leukemia" | 1       | 5        | "disease-ontology" | 6       | "other"   | 10             | "A acute leukemia that is characterized by over production of lymphoblasts." | "http://www.cancer.gov/dictionary?CdrID=46332"

    }

    @Unroll
    def "Disease page - Annotations for #doid"() {
        when:
        def encodedQuery = URLEncoder.encode(doid, "UTF-8")
        def retObj = ApiTester.getApiMetaData("/api/disease/$encodedQuery/genes?limit=250")
        def results = retObj.results
        def ezha = results.find { it.gene.symbol == 'Ezh2' }

        then:
        results //should be some results
        totalResults <= retObj.total
        returned == results.size()
        firstGene == results.first().gene.id
        //firstGeneUrl == results.first().gene.url
        geneSymbol == results.first().gene.symbol
        ezha
        crossRef == ezha.publications[0].id
        evCode == ezha.evidenceCodes[0].name
        disease == ezha.disease.name
        doID == ezha.disease.id
        doURL == ezha.disease.url
        species == ezha.gene.species.name
        where:
        doid        | totalResults | returned | firstGene | geneSymbol | crossRef        | geneticEntityType | evCode                                              | disease                        | doID        | doURL                                           | species
        "DOID:9952" | 66           | 250      | "HGNC:40" | "ABCB1"    | "PMID:22431509" | "gene"            | "author statement supported by traceable reference" | "acute lymphoblastic leukemia" | "DOID:9952" | "http://www.disease-ontology.org/?id=DOID:9952" | "Mus musculus"

    }

    @Unroll
    def "Disease page - Annotations for #sortBy - Sorting"() {
        when:
        def results = ApiTester.getApiResults("/api/disease/DOID:9952/genes?limit=15&sortBy=$sortBy")

        def symbols = results.gene.symbol.findAll { it }
        def species = results.gene.species.name.findAll { it }
        def disease = results.disease.name.findAll { it }

        then:
        results
        symbols.join(",") == geneSymbolList
        species.join(",") == speciesList
        disease.join(",") == diseaseList

        where:
        sortBy    | geneSymbolList                                                                                            | speciesList                                                                                                                                                                                                                                                                                                                                                | diseaseList
        "gene"    | "ABCB1,Abcb1a,Abcb1a,Abcb1b,Abcb1b,abcb4,ABCC2,Abcc2,Abcc2,abcc2,ABCG2,Abcg2,Abcg2,abcg2a,abcg2c"         | "Homo sapiens,Rattus norvegicus,Mus musculus,Rattus norvegicus,Mus musculus,Danio rerio,Homo sapiens,Rattus norvegicus,Mus musculus,Danio rerio,Homo sapiens,Rattus norvegicus,Mus musculus,Danio rerio,Danio rerio"                                                                                                                                       | "acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia"
        "species" | "abl-1,bet-1,bet-2,C45B11.6,C48E7.6,cbp-1,cbp-2,ced-9,ced-9,ces-2,cft-1,cyd-1,cyp-13A1,cyp-13A1,cyp-13A1" | "Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans" | "B-lymphoblastic leukemia/lymphoma,B-lymphoblastic leukemia/lymphoma,B-lymphoblastic leukemia/lymphoma,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,B-lymphoblastic leukemia/lymphoma"
        "disease" | "ABCB1,ABCC2,ABCG2,ABO,AICDA,BAX,BCL2,BCL2L1,BCL2L1,BCR,CAST,CCND1,CD46,CD79B,CD86"                       | "Homo sapiens,Homo sapiens,Homo sapiens,Homo sapiens,Homo sapiens,Homo sapiens,Homo sapiens,Homo sapiens,Homo sapiens,Homo sapiens,Homo sapiens,Homo sapiens,Homo sapiens,Homo sapiens,Homo sapiens"                                                                                                                                                       | "acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia,acute lymphoblastic leukemia"

    }

    @Unroll
    def "Disease page - Annotations for #limit - limit"() {
        when:
        def results = ApiTester.getApiResults("/api/disease/DOID:9952/associations?limit=$limit").results

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
        def retObj = ApiTester.getApiResult("/api/disease/DOID:9952/genes?limit=10&filter.geneName=$geneSymbolQuery")
        def results = retObj.results
        def symbols = results.gene.symbol.findAll { it }

        then:
        results
        resultSize == retObj.total
        geneSymbolList == symbols.join(",")

        where:
        geneSymbolQuery | resultSize | geneSymbolList
        "ot"            | 19         | "DOT1L,NOTCH1,Notch3,otg,NOTCH3,Dot1l,Notch1,Notch3,Dot1l,Notch1"
        "2a"            | 31         | "CDKN2A,CDKN2A,CDKN2A,CDKN2A,CDKN2A,KMT2A,Kmt2a,KMT2A,Cdkn2a,Cdkn2a"
        "r"             | 232        | "AKR1C3,AKR1C3,BCR,BRD2,CARD11,CREBBP,CXCR4,DHFR,DHFR,ERCC1"
    }

    @Unroll
    @Ignore
    def "Gene page - Annotation via Experiment #geneID "() {
        when:
        def retObj = getApiResult("/api/gene/$geneID/diseases-by-experiment?limit=5")
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
    def "Disease page - Annotations including child terms #doID "() {
        when:
        def results = ApiTester.getApiResults("/api/disease/$doID/associations").results

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

    @Unroll
    def "Disease page - based-on inference with multiple models #doID "() {
        when:
        def results = ApiTester.getApiResult("/api/disease/$doID/genes?filter.species=Mus%20musculus&filter.geneName=Cacna1g&filter.associationType=is_implicated_in").results

        then:
        results
        results[0]
        results[0].primaryAnnotatedEntities
        results[0].primaryAnnotatedEntities.size() == paeSize
        where:
        doID        | resultSize | paeSize
        // there should be at least two primary annotated entity
        "DOID:1441" | 1          | 2
    }

    @Unroll
    def "Verify that the downloads endpoint have results"() {
        when:
        // make sure download's endpoint returns all records
        def output = ApiTester.getApiResultRaw("/api/disease/DOID:9952/genes/download?limit=10")
        def results = output.split('\n')

        def outputExperiment = ApiTester.getApiResultRaw("/api/disease/download?geneID=MGI:109583")
        def resultsExperiment = outputExperiment.split('\n')

        then:
        results.size() >= 21
        resultsExperiment.size() > 14

    }

    @Unroll
    def "Check different query parameters #query for disease #disease annotation endpoint"() {
        when:
        def results = ApiTester.getApiResults("/api/disease/$disease/genes?limit=1000&$query")

        then:
        results.size() > resultSizeLowerLimit
        results.size() < resultSizeUpperLimit

        where:
        disease     | query                                             | resultSizeLowerLimit | resultSizeUpperLimit
        "DOID:9952" | ""                                                | 60                   | 1100
        "DOID:9952" | "filter.geneName=2"                               | 25                   | 250
        "DOID:9952" | "filter.disease=cell"                             | 10                   | 210
        "DOID:9952" | "filter.associationType=is_implicated_in"         | 8                    | 110
        "DOID:9952" | "filter.associationType=implicated_via_orthology" | 40                   | 720
        "DOID:9952" | "filter.reference=PMID:1"                         | 0                    | 70
        "DOID:9952" | "filter.reference=MGI:6194"                       | 10                   | 1000
        "DOID:9952" | "filter.evidenceCode=author"                      | 6                    | 20
        "DOID:9952" | "filter.source=gD"                                | 0                    | 1100
        "DOID:9952" | "filter.source=aLLIANc"                           | 50                   | 1100
        "DOID:9952" | "filter.species=Danio%20Rerio"                    | 10                   | 200
    }

/*
    @Unroll
    def "Check disease #disease annotation endpoint for genetic entity type filtering with geneticEntityType filter query #query"() {
        when:
        def results = getApiResults("/api/disease/$disease/associations?limit=100&filter.geneticEntityType=$query")

        then:
        results.size() > resultSizeLowerLimit
        results.size() < resultSizeUpperLimit

        where:
        disease     | query           | resultSizeLowerLimit | resultSizeUpperLimit
        "DOID:9952" | ""              | 60                   | 80
        "DOID:9952" | "gene"          | 60                   | 80
        "DOID:9952" | "allele"        | 3                    | 40
        "DOID:9952" | "allele%7CGENE" | 60                   | 80
    }

*/

}