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
            AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(config.getAWSAccessKey(), config.getAWSSecretKey()))).withRegion(Regions.US_EAST_1).build();

            ObjectListing ol = s3.listObjects(bucketName, prefix);
            log.info(ol.getObjectSummaries().size());
            count = ol.getObjectSummaries().size();
            for (S3ObjectSummary summary : ol.getObjectSummaries()) {

                //if(!summary.getKey().contains("/")) {
                log.info(" - " + summary.getKey() + "\t(size = " + summary.getSize() + ")\t(lastModified = " + summary.getLastModified() + ")");
                //}
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public void saveFile(String path, String data) throws GenericException {
        try {
            Date d = new Date();
            String outFileName = "tmp.data_" + d.getTime();
            File outfile = new File(outFileName);
            FileUtils.writeStringToFile(outfile, data);

            log.info("Saving Path: " + path);
            saveFile(path, outfile);

            outfile.delete();
            log.info("Upload Complete: " + path);
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileSavingException(e.getMessage());
        }
    }

    public void saveFile(String path, File data) throws GenericException {
        try {
            log.info("Uploading: "  + path + " -> s3://" + bucketName + "/" + path);
            AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(config.getAWSAccessKey(), config.getAWSSecretKey()))).withRegion(Regions.US_EAST_1).build();
            //AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(new BasicAWSCredentials("AKIAJYVKAGPPSZ47VJVQ", "1PK/iA0Lk4apNL2Cdov1mY7stgACCFklGEgLk8PL")).withRegion(Regions.US_EAST_1).build();
            TransferManager tm = TransferManagerBuilder.standard().withS3Client(s3).build();
            //TransferManager tm = new TransferManager(new AWSStaticCredentialsProvider(new BasicAWSCredentials("AKIAJYVKAGPPSZ47VJVQ", "1PK/iA0Lk4apNL2Cdov1mY7stgACCFklGEgLk8PL")));

            final Upload uploadFile = tm.upload(bucketName, path, data);
            uploadFile.waitForCompletion();
            tm.shutdownNow();
            log.info("Upload complete.");
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileSavingException(e.getMessage());
        }

    }

}
