import org.alliancegenome.core.translators.document.GeneTranslator
import org.alliancegenome.neo4j.entity.node.GOTerm
import org.alliancegenome.neo4j.repository.GeneRepository
import spock.lang.Specification
import org.alliancegenome.neo4j.entity.node.Gene
import org.alliancegenome.es.index.site.document.GeneDocument
import spock.lang.Shared
import spock.lang.Unroll


class GeneDocumentIntegrationSpec extends Specification {

    @Shared GeneRepository repo
    @Shared GeneTranslator trans

    def setup() {
        repo = new GeneRepository()
        trans = new GeneTranslator()
    }

    def cleanup() {
        repo = null
        trans = null
    }

    def "GeneDocument has GOTerm parents"() {
        when: //we get a gene document
        Gene gene = repo.getOneGene("FB:FBgn0014020")
        GeneDocument geneDocument = trans.translate(gene)

        then: "the document exists and has all 3 root level GO terms"
        geneDocument
        geneDocument.biologicalProcessWithParents
        geneDocument.biologicalProcessWithParents.contains("biological_process")
        geneDocument.cellularComponentWithParents
        geneDocument.cellularComponentWithParents.contains("cellular_component")
        geneDocument.molecularFunctionWithParents
        geneDocument.molecularFunctionWithParents.contains("molecular_function")


    }

}