package org.alliancegenome.indexer.variant.scripts;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.core.config.ConfigHelper;

import com.amazonaws.auth.*;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.PutObjectRequest;
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
        String outputDir = "/Volumes/Cardano_Backup/Variants";

        TransferManager tx = TransferManagerBuilder.standard().withS3Client(s3Client).build();
        
        PutObjectRequest req = new PutObjectRequest(bucket, type + "/" + type + ".vep." + chr + ".vcf.gz", new File(outputDir + "/" + type + ".vep." + chr + ".vcf.gz"));
        S3ProgressListener progress = new S3ProgressListener();
        req.setGeneralProgressListener(progress);
        Upload upload = tx.upload(req);
        progress.setUpload(upload);

        upload.waitForCompletion();

        tx.shutdownNow();
    }

}
