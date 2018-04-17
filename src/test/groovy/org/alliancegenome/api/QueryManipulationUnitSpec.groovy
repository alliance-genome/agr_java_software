import org.alliancegenome.es.util.QueryManipulationService
import spock.lang.Specification
import spock.lang.Shared
import spock.lang.Unroll

class QueryManipulationUnitSpec extends Specification {

    @Shared
    private QueryManipulationService queryManipulationService = new QueryManipulationService()

    @Unroll
    def "#query should be replaced with #output"() {
        when:
        String manipulatedQuery = queryManipulationService.processQuery(query)
        then:
        manipulatedQuery == output

        where:
        query                                 | output
        "DOID:10314"                          | "DOID\\:10314"
        "SNOMEDCT_US_2016_03_01:91357005"     | "SNOMEDCT_US_2016_03_01\\:91357005"
        "UMLS_CUI:C0375268"                   | "UMLS_CUI\\:C0375268"
        "MESH:D004696"                        | "MESH\\:D004696"
        "ICD9CM:421.9"                        | "ICD9CM\\:421.9"
        "FBgn0086442"                         | "FBgn0086442"
        "FB:FBgn0086442"                      | "FB\\:FBgn0086442"
        "ZDB-GENE-001120-2"                   | "ZDB-GENE-001120-2"
        "ZFIN:ZDB-GENE-001120-2"              | "ZFIN\\:ZDB-GENE-001120-2"
        "WBGene00000244"                      | "WBGene00000244"
        "WB:WBGene00000244"                   | "WB\\:WBGene00000244"
        "parkinson's disease"                 | "parkinson's disease"
        "__symbol:fgf8a"                      | "symbol:fgf8a"
        "__primaryId:DOID:10314"              | "primaryId:DOID\\:10314"
        "si:ch211-133d24.26p"                 | "si\\:ch211-133d24.26p"
        "en::ftz::Mmus\\En1"                  | "en\\:\\:ftz\\:\\:Mmus\\En1"
        "Foxn1<sup>nu-StL</sup> (Mmu)"        | "Foxn1\\<sup\\>nu-StL\\<\\/sup\\> (Mmu)"
        "Gt(ROSA)26Sor<sup>tm1(Pik3ca*H1047R)Egan</sup> (Mmu)" | "Gt(ROSA)26Sor\\<sup\\>tm1(Pik3ca*H1047R)Egan\\<\\/sup\\> (Mmu)"
        "rut[EP399] (Dme)"                    | "rut\\[EP399\\] (Dme)"
    }

}
