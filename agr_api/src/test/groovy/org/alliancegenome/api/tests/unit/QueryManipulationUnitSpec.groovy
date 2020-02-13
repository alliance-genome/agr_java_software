
import org.alliancegenome.api.service.QueryManipulationService

import spock.lang.*

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
        "__primaryKey:DOID:10314"             | "primaryKey:DOID\\:10314"
        "si:ch211-133d24.26p"                 | "si\\:ch211-133d24.26p"
        "en::ftz::Mmus\\En1"                  | "en\\:\\:ftz\\:\\:Mmus\\En1"
        "Foxn1<sup>nu-StL</sup> (Mmu)"        | "Foxn1<sup>nu-StL<\\/sup> \\(Mmu\\)"
        "Gt(ROSA)26Sor<sup>tm1(Pik3ca*H1047R)Egan</sup> (Mmu)" | "Gt\\(ROSA\\)26Sor<sup>tm1\\(Pik3ca*H1047R\\)Egan<\\/sup> \\(Mmu\\)"
        "rut[EP399] (Dme)"                    | "rut\\[EP399\\] \\(Dme\\)"
        //auto-quoting hgvs nomenclature
        "NC_000083.6:g.75273979T>A"           | "\"NC_000083.6\\:g.75273979T>A\""
        "NC_005118.4:g.55252024_55252027del"  | "\"NC_005118.4\\:g.55252024_55252027del\""
        "NC_007116.7:g.23258951T>A pax"       | "\"NC_007116.7\\:g.23258951T>A\" pax"
        "cancer NT_033777.3:g.7087759G>A"     | "cancer \"NT_033777.3\\:g.7087759G>A\""
        "\"NT_033777.3:g.7087759G>A\""        | "\"NT_033777.3\\:g.7087759G>A\""
        "\"NC_007116.7:g.23258951T>A\" pax"   | "\"NC_007116.7\\:g.23258951T>A\" pax"
        "NC_000083.6:g.75273979T>A NC_005118.4:g.55252024_55252027del" | "\"NC_000083.6\\:g.75273979T>A\" \"NC_005118.4\\:g.55252024_55252027del\""
        "\"NC_000083.6:g.75273979T>A\" \"NC_005118.4:g.55252024_55252027del\"" | "\"NC_000083.6\\:g.75273979T>A\" \"NC_005118.4\\:g.55252024_55252027del\""
    }

}
