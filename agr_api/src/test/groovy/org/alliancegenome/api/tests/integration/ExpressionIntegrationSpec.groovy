package org.alliancegenome.api

import org.alliancegenome.api.tests.integration.ApiTester
import spock.lang.Specification
import spock.lang.Unroll

class ExpressionIntegrationSpec extends Specification {

    @Unroll
    def "Gene page - Expression Summary for #geneId"() {
        when:
        def encodedQuery = URLEncoder.encode(geneId, "UTF-8")
        def result = ApiTester.getApiResult("/api/gene/$encodedQuery/expression-summary")

        then:
        result
        totalAnnotations <= result.totalAnnotations
        result.groups
        groupCount == result.groups.size()
        anatomyTerms <= result.groups[0].terms.size()
        anatomyTotal <= result.groups[0].totalAnnotations
        CCTerms == result.groups[1].terms.size()
        CCTotal <= result.groups[1].totalAnnotations
        stageTerms == result.groups[2].terms.size()
        stageTotal <= result.groups[2].totalAnnotations

        where:
        geneId                   | totalAnnotations | groupCount | anatomyTerms | anatomyTotal | CCTerms | CCTotal | stageTerms | stageTotal
        "MGI:109583"             | 141              | 3          | 19           | 165          | 17      | 0       | 3          | 141
        "RGD:2129"               | 10               | 3          | 19           | 0            | 17      | 10      | 3          | 0
        "ZFIN:ZDB-GENE-001103-1" | 248              | 3          | 26           | 400          | 17      | 1       | 3          | 245

    }

    @Unroll
    def "Gene page - Expression Annotations for #geneId"() {
        when:
        def results = ApiTester.getApiResults("/api/expression?geneID=$geneId&page=1&limit=10&sortBy=")

        def termNames = results.termName.findAll { it }

        then:
        results
        totalSize <= results.size()
        locationList == termNames.join(',')

        where:
        geneId                   | totalSize | locationList
        "MGI:109583"             | 10        | "2-cell stage embryo,4-cell stage embryo,alimentary system,amnion,amnion,amygdala,axial skeleton,basal ganglia,bladder,brain"
        "RGD:2129"               | 7         | "extracellular space,high-density lipoprotein particle,intracellular membrane-bounded organelle,low-density lipoprotein particle,very-low-density lipoprotein particle,vesicle lumen,vesicle membrane"
        //"ZFIN:ZDB-GENE-001103-1" | 10        | "anal fin,anal fin,anal fin,anal fin pterygiophore,anal fin pterygiophore,brain,brain,caudal fin lepidotrichium,ceratobranchial bone,ceratobranchial cartilage"
    }

    @Unroll
    def "Gene page - Expression Annotations for #geneId : sorted #sortBy"() {
        when:
        def results = ApiTester.getApiResults("/api/expression?geneID=$geneId&page=1&limit=10&sortBy=$sortBy")

        def termNames = results.termName.findAll { it }

        then:
        results
        locationList == termNames.join(',')
        stage == results.stage.name.findAll { it }.join(',')
        where:
        geneId       | sortBy    | locationList                                                                                                                  | stage
        "MGI:109583" | "default" | "2-cell stage embryo,4-cell stage embryo,alimentary system,amnion,amnion,amygdala,axial skeleton,basal ganglia,bladder,brain" | "TS02,TS03,TS22,TS12,TS14,TS28,TS22,TS28,TS23,TS19"
        "MGI:109583" | "assay"   | "amnion,amnion,cartilage,dental sac,epidermis,gut,heart,lung,neural tube,neural tube"                                         | "TS12,TS14,TS24,TS23,TS24,TS16,TS19,TS22,TS12,TS16"
        "MGI:109583" | "stage"   | "2-cell stage embryo,4-cell stage embryo,embryo,embryo,extraembryonic component,amnion,neural tube,somite,trunk,yolk sac"     | "TS02,TS03,TS10,TS10,TS10,TS12,TS12,TS12,TS12,TS12"
    }

    @Unroll
    def "Gene page - Expression Section - orthopicker for #geneId"() {
        when:
        def results = ApiTester.getApiResults("/api/gene/$geneId/homologs-with-expression?stringencyFilter=stringent")

        def homologGenes = results.homologGene.findAll { it }

        then:
        results
        totalSize <= results.size()
        locationList == homologGenes.symbol.join(',')

        where:
        geneId       | totalSize | locationList
        "MGI:109583" | 5         | "daf-18,Pten,Pten,ptena,ptenb,TEP1"
    }

    @Unroll
    def "Verify that the downloads endpoint has results for #gene"() {
        when:
        def result = ApiTester.getApiResultRaw("/api/expression/download?geneID=$gene")
        def results = result.split('\n')

        def resultFilter = getApiResultRaw("/api/expression/download?geneID=$gene&filter.term=ton")
        def resultsFilter = resultFilter.split('\n')

        then:
        results.size() > 10
        results.size() > resultsFilter.size()

        where:
        gene << ["MGI:109583"]
    }

    @Unroll
    def "Gene page: expression section sort by assay for gene #geneID"() {
        when:
        def results = ApiTester.getApiResults("/api/expression?termID=UBERON:AnatomyOtherLocation&geneID=$geneID&geneID=MGI:98872&page=1&limit=100&sortBy=assay")
        def assayNames = results.assay.displaySynonym
        def assayNamesSorted = assayNames.clone().sort { a, b -> a.compareToIgnoreCase b }

        then:
        results //should be some results
        numOfRecords <= results.size()
        assayNames.equals(assayNamesSorted)

        where:
        geneID                   | numOfRecords
        "ZFIN:ZDB-GENE-000210-7" | 10

    }
}
