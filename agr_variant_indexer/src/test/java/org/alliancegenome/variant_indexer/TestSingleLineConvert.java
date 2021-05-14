package org.alliancegenome.variant_indexer;

import java.io.File;
import java.util.*;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.core.variant.converters.AlleleVariantSequenceConverter;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.fasterxml.jackson.databind.ObjectMapper;

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
        
        int count = 0;
        SummaryStatistics ss = new SummaryStatistics();
        
        AlleleVariantSequenceConverter converter = new AlleleVariantSequenceConverter();
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
                List<AlleleVariantSequence> docs = converter.convertContextToAlleleVariantSequence(vc, null, SpeciesType.HUMAN, null);
                
                for(AlleleVariantSequence doc: docs) {
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
