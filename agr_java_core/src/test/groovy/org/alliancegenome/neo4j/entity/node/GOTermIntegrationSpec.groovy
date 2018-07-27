import org.alliancegenome.neo4j.repository.GoRepository
import org.alliancegenome.neo4j.entity.node.GOTerm
import spock.lang.Shared
import spock.lang.Unroll
import spock.lang.Specification

class GOTermIntegrationSpec extends Specification {

    @Shared GoRepository goRepository

    def setup() {
        goRepository = new GoRepository()
    }

    def cleanup() {
        goRepository = null
    }

    @Unroll
    def "#term should have necessary properties"() {
        when: "get a GOTerm"
        GOTerm term = goRepository.getOneGoTerm(termID)

        then: "term & properties shouldn't be null"
        term
        term.getType()
        term.getName()
        term.getNameKey()
        term.getPrimaryKey()

        where:
        termID << ["GO:0004709", "GO:0016301", /*"GO:0005488",*/ "GO:0019907", "GO:1990393", "GO:0007257"]

    }

}