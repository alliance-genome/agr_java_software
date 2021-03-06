package org.alliancegenome.api.tests.integration

import org.alliancegenome.es.model.search.RelatedDataLink
import org.alliancegenome.api.service.SearchService
import spock.lang.*

class RelatedDataServiceIntegrationSpec extends Specification {

    @Shared
    private SearchService searchService = new SearchService()

    @Unroll
    def "#nameKey has #category #targetField related data links"() {
        when:
        RelatedDataLink relatedDataLink = searchService.getRelatedDataLink(category, targetField, nameKey)

        then:
        relatedDataLink
        relatedDataLink.count
        relatedDataLink.count >= min
        relatedDataLink.count <= max

        where:
        nameKey                 | category  | targetField                    | min    | max
        "fgf8a (Dre)"           | "disease" | "genes"                        | 1      | Integer.MAX_VALUE
        "sa2545 (Dre)"          | "gene"    | "alleles"                      | 1      | 1
        "cancer"                | "gene"    | "diseasesWithParents"          | 1000   | Integer.MAX_VALUE
        "extracellular space"   | "gene"    | "cellularComponentWithParents" | 8000   | Integer.MAX_VALUE
    }


}