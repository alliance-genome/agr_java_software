package org.alliancegenome.api

import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll

class OrthologyIT extends Specification {

    @Unroll
    def "Gene page - Homology for #geneId and #stringencyFilter "() {
        when:
        def url = new URL("http://localhost:8080/api/gene/$geneId/homologs?stringencyFilter=$stringencyFilter")
        def retObj = new JsonSlurper().parseText(url.text)
        def results = retObj.results
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
        "MGI:109583"             | "stringent"      | 7         | "Pten"     | "ptenb,ptena,Pten,daf-18,TEP1,Pten,PTEN"
        "FB:FBgn0026379"         | "all"            | 25        | "Pten"     | "TEP1,CDC14,Tns2,Tns3,Cdc14b,Pten,cdc14b,tns2b,tns1a,ptena,tns1b,ptenb,cdc14ab,si:ch211-191a24.3,tns2a,Pten,Tns2,Tns3,CDC14B,TNS2,TNS1,TNS3,PTEN,daf-18,tns-1"
        "ZFIN:ZDB-GENE-990415-8" | "all"            | 10        | "pax2a"    | "PAX2,PAX5,PAX8,egl-38,pax-2,Poxn,eyg,toe,sv,Pax2,Pax8,Pax5,Pax8,Pax2"
    }

}