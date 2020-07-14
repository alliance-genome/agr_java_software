import org.alliancegenome.api.tests.integration.ApiTester
import spock.lang.Specification
import spock.lang.Unroll

class AlleleIntegrationSpec extends Specification {

    @Unroll
    def "Allele #query page: construct section sort genes then nonBGIs "() {
        when:
        def alleleID = URLEncoder.encode(query, "UTF-8")
        def results = ApiTester.getApiResult("/api/allele/$alleleID")

        then:
        results //should be some results
        results.constructs[0].expressedGenes.size() == expressedGenesSize

        where:
        query            | expressedGenesSize
        "FB:FBal0290290" | 4

    }


}