package org.alliancegenome.api

import spock.lang.Unroll

class InteractionIntegrationSpec extends AbstractSpec {

    @Unroll
    def "Gene page - Interaction for #geneId"() {
        when:
        def encodedGeneID = URLEncoder.encode(geneId, "UTF-8")
        def result = getApiResult("/api/gene/$encodedGeneID/interactions?sortBy=InteractorGeneSymbol")

        then:
        result
        firstMoleculeType == result.results[0].interactorAType.displayName
        firstGeneB == result.results[0].geneB.symbol
        firstInteractorType == result.results[0].interactorBType.displayName

        where:
        geneId           | firstMoleculeType | firstGeneB | firstInteractorType
        "FB:FBgn0029891" | "protein"         | "CG11656"  | "protein"
        "MGI:109583"     | "protein"         | "Cdc27"    | "protein"

    }

}
