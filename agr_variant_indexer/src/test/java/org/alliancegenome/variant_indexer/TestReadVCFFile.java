package org.alliancegenome.variant_indexer;

import java.io.File;
import java.util.Date;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TestReadVCFFile {

    public static void main(String[] args) {
        VCFFileReader reader = new VCFFileReader(new File("/Users/olinblodgett/Desktop/Variants/mgp.v5.merged.snps_all.dbSNP142.vcf.gz"), false);
        CloseableIterator<VariantContext> iter1 = reader.iterator();

        Date start = new Date();
        Date end = new Date();
        int record_count = 100000;
        int count = 0;
        
        while(iter1.hasNext()) {
            try {
                VariantContext vc = iter1.next();
                
                
                if(count > 0 && count % record_count == 0) {
                    end = new Date();
                    log.info("Count: " + count + " r/s: " + ((record_count * 1000) / (end.getTime() - start.getTime())));
                    start = new Date();
                }
                count++;
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
