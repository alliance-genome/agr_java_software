package org.alliancegenome.variant_indexer;

import java.io.File;
import java.util.List;

import org.alliancegenome.variant_indexer.util.VariantContextConverter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

public class TestSingleLineConvert {
    public static void main(String[] args) {
        VCFFileReader reader = new VCFFileReader(new File("/Users/olinblodgett/Desktop/Variants/homo_sapiens_incl_consequences-chr1.vcf.gz"), false);
        CloseableIterator<VariantContext> iter1 = reader.iterator();
        
        int count = 0;

        double avg = 0;
        DescriptiveStatistics ds = new DescriptiveStatistics();
        
        while(iter1.hasNext()) {
            try {
                VariantContext vc = iter1.next();
                //if(vc.getID().equals("rs55780505")) {
                List<String> docs = VariantContextConverter.convertVariantContext(vc);
                for(String doc: docs) {
                    
                    ds.addValue(doc.length());
                    count++;
                    //sum += doc.length();
                }
                if(count % 10000 == 0 && count > 0) {
                    System.out.println("Avg: " + ds.getMean() + " SD: " + ds.getStandardDeviation());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
