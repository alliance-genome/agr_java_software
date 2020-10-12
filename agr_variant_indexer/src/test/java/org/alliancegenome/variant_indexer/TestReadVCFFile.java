package org.alliancegenome.variant_indexer;

import java.io.File;

import org.alliancegenome.es.util.ProcessDisplayHelper;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFFileReader;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TestReadVCFFile {

    public static void main(String[] args) {
        VCFFileReader reader = new VCFFileReader(new File("/Users/olinblodgett/Desktop/Variants/WB.vep.vcf.gz"), false);
        CloseableIterator<VariantContext> iter1 = reader.iterator();

        String chr = "";

        ProcessDisplayHelper ph = new ProcessDisplayHelper(2000);
        ph.startProcess("Variants", 1_694_366);
    
        VariantContextWriter vcwb = null;
        
        while(iter1.hasNext()) {
            try {
                VariantContext vc = iter1.next();
                if(!vc.getChr().equals(chr)) {
                    System.out.println("New File: " + chr);
                    if(vcwb != null) {
                        vcwb.close();
                    }
                    vcwb = new VariantContextWriterBuilder().setOutputFile("/Users/olinblodgett/Desktop/Variants/WB.vep.chr" + vc.getChr() + ".vcf.gz").build();
                    vcwb.writeHeader(reader.getFileHeader());
                    chr = vc.getChr();
                }
                vcwb.add(vc);
                
                ph.progressProcess();
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        vcwb.close();
        
        ph.finishProcess();
    }

}
