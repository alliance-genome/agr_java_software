package org.alliancegenome.api.service.helper.git;

import java.io.File;
import java.util.Date;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.alliancegenome.api.config.ConfigHelper;
import org.alliancegenome.api.exceptions.FileSavingException;
import org.alliancegenome.api.exceptions.GenericException;
import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

@ApplicationScoped
public class S3Helper {

    private String bucketName = "mod-datadumps";
    private Logger log = Logger.getLogger(getClass());

    @Inject
    private ConfigHelper config;

    public int listFiles(String prefix) {
        int count = 0;
        try {
            log.info("Getting S3 file listing");
            AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(config.getAWSAccessKey(), config.getAWSSecretKey()))).withRegion(Regions.US_EAST_1).build();
            ObjectListing ol = s3.listObjects(bucketName, prefix);
            log.debug(ol.getObjectSummaries().size());
            count = ol.getObjectSummaries().size();
            for (S3ObjectSummary summary : ol.getObjectSummaries()) {
                log.debug(" - " + summary.getKey() + "\t(size = " + summary.getSize() + ")\t(lastModified = " + summary.getLastModified() + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public void saveFile(String path, String fileData) throws GenericException {
        try {
            Date d = new Date();
            String outFileName = "tmp.data_" + d.getTime();
            File outfile = new File(outFileName);
            log.info("Saving file to local filesystem: " + outfile.getAbsolutePath());
            FileUtils.writeStringToFile(outfile, fileData);
            log.info("Save file to local filesystem complete");

            log.info("Uploading file to S3: " + outfile.getAbsolutePath() + " -> s3://" + bucketName + "/" + path);
            AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(config.getAWSAccessKey(), config.getAWSSecretKey()))).withRegion(Regions.US_EAST_1).build();
            TransferManager tm = TransferManagerBuilder.standard().withS3Client(s3).build();
            final Upload uploadFile = tm.upload(bucketName, path, outfile);
            uploadFile.waitForCompletion();
            tm.shutdownNow();
            outfile.delete();
            log.info("S3 Upload complete");
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileSavingException(e.getMessage());
        }
    }

}
