package org.alliancegenome.api

import spock.lang.Unroll

class ExpressionIntegrationSpec extends AbstractSpec {

    @Unroll
    def "Gene page - Expression Summary for #geneId"() {
        when:
        def encodedQuery = URLEncoder.encode(geneId, "UTF-8")
        def result = getApiResult("/api/gene/$encodedQuery/expression-summary")
        println result.groups[0].terms.name
        then:
        result
        totalAnnotations <= result.totalAnnotations
        result.groups
        groupCount == result.groups.size()
        anatomyTerms == result.groups[0].terms.size()
        anatomyTotal <= result.groups[0].totalAnnotations
        CCTerms == result.groups[1].terms.size()
        CCTotal <= result.groups[1].totalAnnotations
        stageTerms == result.groups[2].terms.size()
        stageTotal <= result.groups[2].totalAnnotations

        where:
        geneId                   | totalAnnotations | groupCount | anatomyTerms | anatomyTotal | CCTerms | CCTotal | stageTerms | stageTotal
        "MGI:109583"             | 306              | 3          | 26           | 165          | 17      | 0       | 3          | 141
        "RGD:2129"               | 10               | 3          | 26           | 0            | 17      | 10      | 3          | 0
        "ZFIN:ZDB-GENE-001103-1" | 646              | 3          | 26           | 400          | 17      | 1       | 3          | 245

    }

    @Unroll
    def "Gene page - Expression Annotations for #geneId"() {
        when:
        def results = getApiResults("/api/expression?geneID=$geneId&page=1&limit=10&sortBy=")

        def termNames = results.termName.findAll { it }

        then:
        results
        totalSize <= results.size()
        locationList == termNames.join(',')

        where:
        geneId                           | totalSize | locationList
        "MGI:109583"                     | 10        | "2-cell stage embryo,4-cell stage embryo,alimentary system,amnion,amnion,amygdala,axial skeleton,basal ganglia,bladder,brain"
        "RGD:2129"                       | 7         | "extracellular space,high-density lipoprotein particle,intracellular membrane-bounded organelle,low-density lipoprotein particle,very-low-density lipoprotein particle,vesicle lumen,vesicle membrane"
        "ZFIN:ZDB-GENE-001103-1"         | 10        | "anal fin,anal fin,anal fin,anal fin pterygiophore,anal fin pterygiophore,brain,brain,caudal fin lepidotrichium,ceratobranchial bone,ceratobranchial cartilage"
    }

}
