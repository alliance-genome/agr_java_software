package groovy.org.alliancegenome.api

import groovy.json.JsonSlurper
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class OrthologyWebServiceSpec extends Specification {

    //@Ignore("Not working until we get test data into travis")
    @Unroll
    def "When querying for #species "() {
        when:
        def url = new URL("http://localhost:8080/api/orthology/$species")
        def jsonResponseResult = new JsonSlurper().parseText(url.text)
        def results = jsonResponseResult.results
        def total = jsonResponseResult.total
        def Set<String> genePair = new HashSet<>()
        def Set<String> geneIdPair = new HashSet<>()
        for(Map map: results){
            String gene = map.get("gene").get("symbol")
            String homologGene = map.get("homologGene").get("symbol")
            String geneId = map.get("gene").get("primaryKey")
            String homologGeneId = map.get("homologGene").get("primaryKey")
            genePair.add(gene+" | "+homologGene);
            geneIdPair.add(geneId+" | "+homologGeneId);
        }
        then:
        total > numberOfRecords
        genePair.contains(pairName)
        geneIdPair.contains(pairID)

        where:
        species | numberOfRecords | pairName         | pairID
        "10090" | 5               | "Pten | ptenb"   | "MGI:109583 | ZFIN:ZDB-GENE-030616-47"
    }

    //@Ignore("Not working until we get test data into travis")
    @Unroll
    def "When querying for #species1 and #species2 "() {
        when:
        def url = new URL("http://localhost:8080/api/orthology/$species1/$species2")
        def jsonResponseResult = new JsonSlurper().parseText(url.text)
        def results = jsonResponseResult.results
        def total = jsonResponseResult.total
        def Set<String> genePair = new HashSet<>()
        def Set<String> geneIdPair = new HashSet<>()
        for(Map map: results){
            String gene = map.get("gene").get("symbol")
            String homologGene = map.get("homologGene").get("symbol")
            String geneId = map.get("gene").get("primaryKey")
            String homologGeneId = map.get("homologGene").get("primaryKey")
            genePair.add(gene+" | "+homologGene);
            geneIdPair.add(geneId+" | "+homologGeneId);
        }
        then:
        total > numberOfRecords
        genePair.contains(pairName)
        geneIdPair.contains(pairID)

        where:
        species1 | species2  | numberOfRecords | pairName         | pairID
        "10090"  | "10116"   | 5               | "Pten | Pten"    | "RGD:61995 | MGI:109583"
    }

}