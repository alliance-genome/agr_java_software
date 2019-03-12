import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll

class CategoriesAndFieldsIntegrationForLaterSpec extends Specification {


    @Unroll
    def "There should be some #category documents"() {
        when: "we search for documents from a category"
        def total = search("category:$category")?.hits?.total
        then: "some should come back"
        total
        total > 0

        where:
        category << ["gene", "disease", "go", "allele"]
    }

    @Unroll
    def "#category docs should have values in #field"() {
        when: "when we execute a search for documents with values in a field within a category"
        def query = "category:$category AND $field:[* TO *]"
        def total = search(query)?.hits?.total

        then: "there should be hits"
        total
        total > 0

        where:
        category  | field

        "gene"    | "primaryId"
        "gene"    | "secondaryIds"
        "gene"    | "category"
        "gene"    | "name"
        "gene"    | "name.keyword"
        "gene"    | "name.autocomplete"
        "gene"    | "synonyms"
        "gene"    | "synonyms.autocomplete"
        "gene"    | "symbol"
        "gene"    | "symbol.autocomplete"
        "gene"    | "species"
        "gene"    | "species.synonyms"
        "gene"    | "soTermName"
        "gene"    | "biologicalProcessWithParents"
        "gene"    | "molecularFunctionWithParents"
        "gene"    | "cellularComponentWithParents"
        "gene"    | "biologicalProcessAgrSlim"
        "gene"    | "molecularFunctionAgrSlim"
        "gene"    | "automatedGeneSynopsis"
        "gene"    | "strictOrthologySymbols"
        "gene"    | "anatomicalExpression"
        "gene"    | "anatomicalExpressionWithParents"
        "gene"    | "phenotypeStatements"
        "gene"    | "alleles"
        "gene"    | "cellularComponentExpressionAgrSlim"
        "gene"    | "cellularComponentExpressionWithParents"

        "go"      | "primaryId"
        "go"      | "category"
        "go"      | "name"
        "go"      | "name.keyword"
        "go"      | "name.autocomplete"
        "go"      | "synonyms"
        "go"      | "synonyms.autocomplete"

        "disease" | "primaryId"
        "disease" | "category"
        "disease" | "name"
        "disease" | "name.keyword"
        "disease" | "name.autocomplete"
        "disease" | "synonyms"
        "disease" | "synonyms.autocomplete"
        "disease" | "associatedSpecies"

        "allele"  | "primaryKey"
        "allele"  | "category"
        "allele"  | "name"
        "allele"  | "name.keyword"
        "allele"  | "name.autocomplete"
        "allele"  | "synonyms"
        "allele"  | "synonyms.autocomplete"
        "allele"  | "species"
        "allele"  | "genes"
        "allele"  | "diseases"

    }


    private def search(String query) {
        //todo: need to set the ES url in a nicer way
        query = URLEncoder.encode(query, "UTF-8")
        def url = new URL("http://localhost:9200/site_index/_search?q=$query")
        return new JsonSlurper().parseText(url.text)
    }
}