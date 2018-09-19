import org.alliancegenome.core.translators.document.GeneTranslator
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

    @Unroll
    def "GeneDocument for #geneID has expressionBioEntities #entities"() {
        when:
        Gene gene = repo.getOneGene(geneID)
        GeneDocument geneDocument = trans.translate(gene)

        then:
        geneDocument
        geneDocument.getWhereExpressed
        geneDocument.getWhereExpressed.containsAll(entities)

        where:
        geneID                      | entities
        "ZFIN:ZDB-GENE-010323-11"   | ["paraxial mesoderm", "somite"]
        "ZFIN:ZDB-GENE-030131-7696" | ["whole organism", "head", "hair cell apical region"]

    }

    @Unroll
    def "GeneDocument for #geneID has UBERON anitomicalExpression for #entities"() {
        when:
        Gene gene = repo.getOneGene(geneID)
        GeneDocument geneDocument = trans.translate(gene)

        then:
        geneDocument
        geneDocument.getAnatomicalExpression()
        geneDocument.getAnatomicalExpression().containsAll(entities)

        where:
        geneID                      | entities
        "ZFIN:ZDB-GENE-030131-7696" | ["visual system", "sensory system", "nervous system"]

    }

    @Unroll
    def "GeneDocument for #geneID has cellularComponentExpression for #entities"() {
        when:
        Gene gene = repo.getOneGene(geneID)
        GeneDocument geneDocument = trans.translate(gene)

        then:
        geneDocument
        geneDocument.getCellularComponentExpression()
        geneDocument.getCellularComponentExpression().containsAll(entities)

        where:
        geneID                      | entities
        "ZFIN:ZDB-GENE-030131-7696" | ["axon", "photoreceptor inner segment", "presynaptic cytosol"]

    }

    def "#geneID has #strict in strict list, but not #otherOrthologue"() {
        when:
        Gene gene = repo.getOneGene(geneID)
        GeneDocument geneDocument = trans.translate(gene)

        then:
        geneDocument

        where:
        geneID                    | strictOrthologue | otherOrthologue
        "ZFIN:ZDB-GENE-010323-11" | "ena"            | "Y20F4.4"
        "ZFIN:ZDB-GENE-010323-11" | "ENAH"           | "EVL"
    }

}