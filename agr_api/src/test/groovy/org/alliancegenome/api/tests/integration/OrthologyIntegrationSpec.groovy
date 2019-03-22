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
        orthologousGenes == results.homologGene.symbol.join(',')


        where:
        geneId                   | stringencyFilter | totalSize | geneSymbol | orthologousGenes
        "MGI:109583"             | "stringent"      | 7         | "Pten"     | "ptenb,ptena,daf-18,Pten,PTEN,TEP1,Pten"
        "FB:FBgn0026379"         | "all"            | 25        | "Pten"     | "TEP1,CDC14,daf-18,tns-1,cdc14b,tns2b,tns1a,ptena,tns1b,ptenb,cdc14ab,si:ch211-191a24.3,tns2a,TNS2,CDC14B,PTEN,TNS3,TNS1,Pten,Cdc14b,Tns2,Tns3,Tns3,Tns2,Pten"
        "ZFIN:ZDB-GENE-990415-8" | "all"            | 10        | "pax2a"    | "PAX5,PAX2,PAX8,eyg,toe,Poxn,sv,Pax2,Pax5,Pax8,Pax2,Pax8,egl-38,pax-2"
    }

}