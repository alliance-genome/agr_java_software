package org.alliancegenome.variant_indexer;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.core.config.ConfigHelper;

import com.amazonaws.auth.*;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.transfer.*;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class TestUploadFile {

    public static void main(String[] args) throws Exception {
        ConfigHelper.init();
        String type = "HUMAN";
        String chr = "1";
        String bucket = "mod-datadumps";
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(ConfigHelper.loadSystemENVProperty("AWS_ACCESS_KEY"), ConfigHelper.loadSystemENVProperty("AWS_SECRET_KEY"));
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        String outputDir = "/Users/olinblodgett/git/agr_java_software/agr_variant_indexer/data";

        TransferManager tx = TransferManagerBuilder.standard().withS3Client(s3Client).build();
        Upload upload = tx.upload(bucket, type + "/" + type + ".vep." + chr + ".vcf.gz", new File(outputDir + "/" + type + ".vep." + chr + ".vcf.gz"));

        int lastPct = 0;
        while(!upload.isDone()) {
            int pct = (int)upload.getProgress().getPercentTransferred();
            if(lastPct != pct) {
                log.info("Percent: " + pct);
                lastPct = pct;
            } else {
                TimeUnit.MILLISECONDS.sleep(500);
            }
        }
        
        upload.waitForCompletion();

        tx.shutdownNow();
    }

}
