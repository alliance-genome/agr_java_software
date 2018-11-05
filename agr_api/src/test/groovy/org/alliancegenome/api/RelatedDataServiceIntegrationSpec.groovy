package org.alliancegenome.api

import org.alliancegenome.es.model.search.RelatedDataLink
import org.alliancegenome.api.service.RelatedDataService
import spock.lang.*


class RelatedDataServiceIntegrationSpec extends Specification {

    @Shared
    private RelatedDataService relatedDataService = new RelatedDataService()

    @Unroll
    def "#primaryKey has #category #targetField related data links"() {
        when:
        RelatedDataLink relatedDataLink = relatedDataService.getRelatedDataLink(category, targetField, nameKey)

        then:
        relatedDataLink
        relatedDatalink.count
        relatedDataLink.count > 0

        where:
        nameKey        |  category   | targetField
        "fgf8a (Dre)"  | "go"        | "biologicalProcessAgrSlim"
    }


}