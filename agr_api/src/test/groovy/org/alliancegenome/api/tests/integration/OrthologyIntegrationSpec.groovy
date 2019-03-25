package org.alliancegenome.api

import spock.lang.Unroll

class OrthologyIntegrationSpec extends AbstractSpec {

    @Unroll
    def "Gene page - Homology for #geneId and #stringencyFilter "() {
        when:
        def results = getApiResults("/api/gene/$geneId/homologs?stringencyFilter=$stringencyFilter")

        def stringencyNames = results.stringencyFilter.findAll { it }

        then:
        results
        totalSize <= results.size()
        results.stringencyFilter
        // make sure all stringencyFilter attributes exist in the JSON
        results.stringencyFilter.each {
            it != null
        }
        geneSymbol == results[0].gene.symbol
        results.homologGene.symbol.contains(orthologousGenes)


        where:
        geneId                   | stringencyFilter | totalSize | geneSymbol | orthologousGenes
        "MGI:109583"             | "stringent"      | 7         | "Pten"     | "daf-18"
        "FB:FBgn0026379"         | "all"            | 25        | "Pten"     | "cdc14ab"
        "ZFIN:ZDB-GENE-990415-8" | "all"            | 10        | "pax2a"    | "egl-38"
    }

}