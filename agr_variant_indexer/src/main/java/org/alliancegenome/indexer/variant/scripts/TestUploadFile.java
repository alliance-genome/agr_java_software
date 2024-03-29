package org.alliancegenome.indexer.variant.scripts;

import java.io.File;

import org.alliancegenome.core.config.ConfigHelper;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
