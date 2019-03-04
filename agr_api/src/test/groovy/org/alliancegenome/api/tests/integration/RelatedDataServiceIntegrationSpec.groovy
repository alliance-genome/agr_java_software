package org.alliancegenome.api

import org.alliancegenome.es.model.search.RelatedDataLink
import org.alliancegenome.api.service.SearchService
import spock.lang.*


class RelatedDataServiceIntegrationSpec extends Specification {

    @Shared
    private SearchService searchService = new SearchService()

    @Unroll
    def "#primaryKey has #category #targetField related data links"() {
        when:
        RelatedDataLink relatedDataLink = searchService.getRelatedDataLink(category, targetField, nameKey)

        then:
        relatedDataLink
        relatedDataLink.count
        relatedDataLink.count > 0

        where:
        nameKey        |  category        | targetField
        "fgf8a (Dre)"  | "disease"        | "genes"
    }


}