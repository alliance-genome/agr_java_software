package org.alliancegenome.variant_indexer;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.util.ProcessDisplayHelper;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.*;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.transfer.*;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.*;
import htsjdk.variant.vcf.VCFFileReader;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class TestDownloadSplitUploadHuman {

    
    private AmazonS3 s3Client;

    public static void main(String[] args) throws Exception {
        new TestDownloadSplitUploadHuman();
    }

    public TestDownloadSplitUploadHuman() {
        ConfigHelper.init();
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(ConfigHelper.loadSystemENVProperty("AWS_ACCESS_KEY"), ConfigHelper.loadSystemENVProperty("AWS_SECRET_KEY"));
        s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        
        String type = "HUMAN";
        String bucket = "mod-datadumps";
        String inputDir =  "/Users/olinblodgett/git/agr_java_software/agr_variant_indexer/data";
        String outputDir = "/Users/olinblodgett/git/agr_java_software/agr_variant_indexer/data";

        String variantFile = type + "_HTPOSTVEPVCF_20210527.vcf.gz";

        File inputFile = new File(inputDir + "/" + variantFile);

        if(!inputFile.exists()) {

            try {
                TransferManager tx = TransferManagerBuilder.standard().withS3Client(s3Client).build();
                Download myDownload = tx.download(bucket, "variants/" + variantFile, inputFile);

                int lastPct = 0;
                while(!myDownload.isDone()) {
                    int pct = (int)myDownload.getProgress().getPercentTransferred();
                    if(lastPct != pct) {
                        log.info("Percent: " + pct);
                        lastPct = pct;
                    } else {
                        TimeUnit.MILLISECONDS.sleep(500);
                    }
                }

                myDownload.waitForCompletion();
                tx.shutdownNow();
            } catch (InterruptedException | AmazonClientException e) {
                e.printStackTrace();
            }
        }


        VCFFileReader reader = new VCFFileReader(inputFile, false);

        String chr = "";

        VariantContextWriter writer = null;

        ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);

        CloseableIterator<VariantContext> iter1 = reader.iterator();

        ph.startProcess("VCFReader");

        TransferManager tx = TransferManagerBuilder.standard().withS3Client(s3Client).build();
        List<Upload> uploads = new ArrayList<>();

        while(iter1.hasNext()) {
            try {
                VariantContext vc = iter1.next();
                if(!vc.getChr().equals(chr)) {
                    if(writer != null) {
                        log.info("Closing: " + outputDir + "/" + type + ".vep." + chr + ".vcf.gz");
                        writer.close();
                        log.info("Uploading new file: " + outputDir + "/" + type + ".vep." + chr + ".vcf.gz");
                        Upload upload = tx.upload(bucket, type + "/" + type + ".vep." + chr + ".vcf.gz", new File(outputDir + "/" + type + ".vep." + chr + ".vcf.gz"));
                        uploads.add(upload);
                    }
                    chr = vc.getContig();
                    VariantContextWriterBuilder builder = new VariantContextWriterBuilder();
                    log.info("Opening New file: " + outputDir + "/" + type + ".vep." + chr + ".vcf.gz");
                    builder.setOutputFile(outputDir + "/" + type + ".vep." + chr + ".vcf.gz");
                    writer = builder.build();
                    writer.writeHeader(reader.getFileHeader());
                }
                writer.add(vc);
                ph.progressProcess();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.info("Waiting for uploads to finish: ");
        for(Upload u: uploads) {
            try {
                u.waitForCompletion();
            } catch (AmazonClientException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("Uploads finished shuting down");
        tx.shutdownNow();
        writer.close();
        ph.finishProcess();
    }
}
