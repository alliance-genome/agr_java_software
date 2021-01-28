package org.alliancegenome.variant_indexer;

import java.io.File;

import org.alliancegenome.es.util.ProcessDisplayHelper;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.*;
import htsjdk.variant.vcf.VCFFileReader;

public class TestSplitFileByChromosome {
    public static void main(String[] args) {
        VCFFileReader reader = new VCFFileReader(new File("/Users/olinblodgett/HTPOSTVEPVCF_ZFIN_2.vcf.gz"), false);

        String chr = "";

        VariantContextWriter writer = null;
        
        ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);
        
        CloseableIterator<VariantContext> iter1 = reader.iterator();
        
        ph.startProcess("VCFReader Reader: ", 34823734);
        
        while(iter1.hasNext()) {
            try {
                VariantContext vc = iter1.next();
                if(!vc.getChr().equals(chr)) {
                    if(writer != null) writer.close();
                    chr = vc.getChr();
                    VariantContextWriterBuilder builder = new VariantContextWriterBuilder();
                    builder.setOutputFile("/Volumes/Cardano_Backup/Variants/ZFIN.vep.chr" + chr + ".vcf.gz");
                    writer = builder.build();
                    writer.writeHeader(reader.getFileHeader());
                }
                writer.add(vc);
                ph.progressProcess();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        writer.close();
        ph.finishProcess();
    }
}
