package org.alliancegenome.api


import spock.lang.Unroll

class DiseaseAnnotationIntegrationSpec extends AbstractSpec {

    @Unroll
    def "Disease page for #query"() {
        when:
        def encodedQuery = URLEncoder.encode(query, "UTF-8")
        //todo: need to set the base search url in a nicer way
        def disease = getApiResult("/api/disease/$encodedQuery")

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
        def retObj = getApiMetaData("/api/disease/$encodedQuery/associations?limit=50")
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
        ezha.allele.symbol != null
        ezha.allele.url != null
        crossRef == ezha.publications[0].id
        geneticEntityType == ezha.geneticEntityType
        evCode == ezha.evidenceCodes[0].name
        disease == ezha.disease.name
        doID == ezha.disease.id
        doURL == ezha.disease.url
        species == ezha.gene.species.name
        where:
        doid        | totalResults | returned | firstGene    | geneSymbol | crossRef        | geneticEntityType | evCode | disease                      | doID        | doURL                                           | species
        "DOID:9952" | 66           | 50       | "HGNC:24948" | "DOT1L"    | "PMID:22431509" | "allele"          | "TAS"  | "acute lymphocytic leukemia" | "DOID:9952" | "http://www.disease-ontology.org/?id=DOID:9952" | "Mus musculus"

    }

    @Unroll
    def "Disease page - Annotations for #sortBy - Sorting"() {
        when:
        def results = getApiResults("/api/disease/DOID:9952/associations?limit=15&sortBy=$sortBy")

        def symbols = results.gene.symbol.findAll { it }
        def species = results.gene.species.name.findAll { it }
        def disease = results.disease.name.findAll { it }

        then:
        results
        symbols.join(",") == geneSymbolList
        species.join(",") == speciesList
        disease.join(",") == diseaseList

        where:
        sortBy       | geneSymbolList                                                                                      | speciesList                                                                                                                                                                                                                                                                                                    | diseaseList
        "geneSymbol" | "Bx,ces-2,ces-2,CG7786,cntn2,CNTN2,Cntn2,Cntn2,Cont,daf-18,DBP,Dbp,Dbp,dot-1.1,dot-1.2"             | "Drosophila melanogaster,Caenorhabditis elegans,Caenorhabditis elegans,Drosophila melanogaster,Danio rerio,Homo sapiens,Mus musculus,Rattus norvegicus,Drosophila melanogaster,Caenorhabditis elegans,Homo sapiens,Mus musculus,Rattus norvegicus,Caenorhabditis elegans,Caenorhabditis elegans"               | "acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,T-cell adult acute lymphocytic leukemia,T-cell adult acute lymphocytic leukemia,T-cell adult acute lymphocytic leukemia,T-cell adult acute lymphocytic leukemia,T-cell adult acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,B- and T-cell mixed leukemia,B- and T-cell mixed leukemia"
        "species"    | "ces-2,ces-2,daf-18,dot-1.1,dot-1.2,dot-1.4,dot-1.5,glp-1,lin-12,mes-2,zag-1,cntn2,dot1l,ezh2,hlfa" | "Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Caenorhabditis elegans,Danio rerio,Danio rerio,Danio rerio,Danio rerio" | "acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,B- and T-cell mixed leukemia,B- and T-cell mixed leukemia,B- and T-cell mixed leukemia,B- and T-cell mixed leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,T-cell adult acute lymphocytic leukemia,T-cell adult acute lymphocytic leukemia,B- and T-cell mixed leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia"
        "disease"    | "Bx,ces-2,ces-2,CG7786,daf-18,DBP,Dbp,Dbp,E(z),Ezh2,Ezh2,ezh2,EZH2,Ezh2,Ezh2"                       | "Drosophila melanogaster,Caenorhabditis elegans,Caenorhabditis elegans,Drosophila melanogaster,Caenorhabditis elegans,Homo sapiens,Mus musculus,Rattus norvegicus,Drosophila melanogaster,Mus musculus,Mus musculus,Danio rerio,Homo sapiens,Mus musculus,Rattus norvegicus"                                   | "acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia,acute lymphocytic leukemia"

    }

    @Unroll
    def "Disease page - Annotations for #limit - limit"() {
        when:
        def results = getApiResults("/api/disease/DOID:9952/associations?limit=$limit").results

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
        def retObj = getApiResult("/api/disease/DOID:9952/associations?limit=50&filter.geneName=$geneSymbolQuery")
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
    def "Disease page - Annotation Filtering for geneticEntity "() {
        when:
        def geneRetObj = getApiMetaData("/api/disease/DOID:9952/associations?limit=50&filter.geneticEntityType=gene")
        def alleleRetObj = getApiMetaData("/api/disease/DOID:9952/associations?limit=50&filter.geneticEntityType=allele")
        def totalAll = getApiMetaData("/api/disease/DOID:9952/associations?limit=50")

        then:
        geneRetObj
        alleleRetObj.total > 3
        totalAll
        // alleles and genes add up to the total
        totalAll.total == geneRetObj.total + alleleRetObj.total

    }

    @Unroll
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
    def "Gene page - Annotation via Orthology #geneID "() {
        when:
        def retObj = getApiResult("/api/gene/$geneID/diseases-via-orthology?limit=5")
        def results = retObj.results

        then:
        results
        retObj.total >= resultSize
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
        def results = getApiResults("/api/disease/$doID/associations").results

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
    def "Verify that the downloads endpoint have results"() {
        when:
        def output = getApiResultRaw("/api/disease/DOID:9952/associations/download?limit=10")
        def results = output.split('\n')

        def outputExperiment = getApiResultRaw("/api/gene/MGI:109583/diseases-by-experiment/download?page=1&limit=100&sortBy=disease")
        def resultsExperiment = outputExperiment.split('\n')

        def outputOrtho = getApiResultRaw("/api/gene/MGI:109583/diseases-via-orthology/download?page=1&limit=100&sortBy=disease")
        def resultsOrtho = outputOrtho.split('\n')

        then:
        results.size() > 70
        resultsExperiment.size() > 48
        resultsExperiment.size() > 48
        resultsOrtho.size() > 30

    }

    @Unroll
    def "Check different query parameters #query for disease #disease annotation endpoint"() {
        when:
        def results = getApiResults("/api/disease/$disease/associations?limit=1000&$query")

        then:
        results.size() > resultSizeLowerLimit
        results.size() < resultSizeUpperLimit

        where:
        disease     | query                             | resultSizeLowerLimit | resultSizeUpperLimit
        "DOID:9952" | ""                                | 60                   | 80
        "DOID:9952" | "filter.geneName=2"               | 30                   | 40
        "DOID:9952" | "filter.disease=cell"             | 10                   | 40
        "DOID:9952" | "filter.geneticEntity=tm"         | 0                    | 10
        "DOID:9952" | "filter.geneticEntityType=allele" | 0                    | 10
        "DOID:9952" | "filter.associationType=is"       | 10                   | 80
        "DOID:9952" | "filter.associationType=OrTHo"    | 40                   | 80
        "DOID:9952" | "filter.reference=PMID:1"         | 0                    | 50
        "DOID:9952" | "filter.reference=MGI:6194"       | 10                   | 70
        "DOID:9952" | "filter.evidenceCode=As"          | 10                   | 20
        "DOID:9952" | "filter.source=gD"                | 0                    | 10
        "DOID:9952" | "filter.source=aLLIANc"           | 50                   | 70
        "DOID:9952" | "filter.species=ELeG"             | 10                   | 20
    }

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


}