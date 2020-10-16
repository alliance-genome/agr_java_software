package org.alliancegenome.variant_indexer;

import java.io.File;
import java.util.*;

import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.variant_indexer.converters.human.HumanVariantContextConverter;
import org.alliancegenome.variant_indexer.es.model.VariantDocument;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.*;

public class TestSingleLineConvert {
    public static void main(String[] args) {
        VCFFileReader reader = new VCFFileReader(new File("/Volumes/Cardano_Backup/Variants/HUMAN.v2.vep.chr9.vcf.gz"), false);
        CloseableIterator<VariantContext> iter1 = reader.iterator();

        ObjectMapper mapper = new ObjectMapper();
        
        VCFInfoHeaderLine header = reader.getFileHeader().getInfoHeaderLine("CSQ");
        System.out.println(header.getDescription());
        String[] formats = header.getDescription().split("Format: ")[1].split("\\|");
        
        for(int i = 0; i < formats.length; i++) {
            formats[i] = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, formats[i]);
        }
        
        int count = 0;
        SummaryStatistics ss = new SummaryStatistics();
        
        HumanVariantContextConverter converter = new HumanVariantContextConverter();
        Date start = new Date();
        Date end = new Date();
        double avg = 0;
        double len = 4;
        double rate = 0;
        double total = 0;
        while(iter1.hasNext()) {
            try {
                VariantContext vc = iter1.next();
                //if(vc.getID().equals("rs55780505")) {
                List<VariantDocument> docs = converter.convertVariantContext(vc, SpeciesType.HUMAN, formats);
                
                for(VariantDocument doc: docs) {
                    String jsonDoc = mapper.writeValueAsString(doc);
                    if(count == 10000) {
                        end = new Date();
                        rate = (total) / (end.getTime() - start.getTime());
                        avg = (avg - (avg / len)) + (rate / len);
                        System.out.println("Running Average (" + len + "): " + avg + " Rate: " + rate + " Total: " + total);
                        ss = new SummaryStatistics();
                        count = 0;
                    }
                    ss.addValue(jsonDoc.length());
                    count++;
                    //sum += doc.length();
                    total++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        end = new Date();
        System.out.println("Final Rate: " + (total / (end.getTime() - start.getTime())));

    }
}
