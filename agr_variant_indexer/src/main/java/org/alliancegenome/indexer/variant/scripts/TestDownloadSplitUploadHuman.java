package org.alliancegenome.indexer.variant.scripts;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.util.ProcessDisplayHelper;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.*;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.*;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.*;
import htsjdk.variant.vcf.*;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class TestDownloadSplitUploadHuman {

    private AmazonS3 s3Client;
    private TransferManager uploadTX;
    private String type = "HUMAN";
    private String bucket = "mod-datadumps";
    private List<Upload> uploads = new ArrayList<>();
    
    public static void main(String[] args) throws Exception {
        new TestDownloadSplitUploadHuman(args);
    }

    public TestDownloadSplitUploadHuman(String[] args) {
        ConfigHelper.init();
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(ConfigHelper.loadSystemENVProperty("AWS_ACCESS_KEY"), ConfigHelper.loadSystemENVProperty("AWS_SECRET_KEY"));
        s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();

        uploadTX = TransferManagerBuilder.standard().withS3Client(s3Client).build();

        String inputDir =  "/Users/olinblodgett/git/agr_java_software/agr_variant_indexer/data";
        String outputDir = "/Volumes/Cardano_Backup/Variants";
        
        if(args.length > 1) {
            inputDir = args[0];
            outputDir = args[1];
            try {
                log.info("Checking Input Dir: " + inputDir);
                log.info("Checking Output Dir: " + outputDir);
                File intputDirectory = new File(inputDir);
                File outputDirectory = new File(outputDir);
                if(!intputDirectory.isDirectory() || !outputDirectory.isDirectory()) {
                    if(!intputDirectory.isDirectory()) {
                        log.error("Input Directory: " + inputDir + " does not exist please create");
                    }
                    if(!outputDirectory.isDirectory()) {
                        log.error("Output Directory: " + outputDir + " does not exist please create");
                    }
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        String variantFile = type + "_HTPOSTVEPVCF_20211214.vcf.gz";

        File inputFile = new File(inputDir + "/" + variantFile);

        if(!inputFile.exists()) {

            try {
                TransferManager tx = TransferManagerBuilder.standard().withS3Client(s3Client).build();
                Download myDownload = tx.download(bucket, "variants/" + variantFile, inputFile);

                int lastPct = 0;
                while(!myDownload.isDone()) {
                    int pct = (int)myDownload.getProgress().getPercentTransferred();
                    if(lastPct != pct) {
                        log.info(variantFile + ": Percent: " + pct);
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
        ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);

        CloseableIterator<VariantContext> iter1 = reader.iterator();

        ph.startProcess("VCFReader");

        List<Thread> threads = new ArrayList<>();
        WriterThread current = null;
        String chr = "";
        while(iter1.hasNext()) {
            try {
                VariantContext vc = iter1.next();
                if(!vc.getContig().equals(chr)) {
                    chr = vc.getContig();
                    WriterThread thread = new WriterThread(reader.getFileHeader(), outputDir, type, chr);
                    thread.start();
                    threads.add(thread);
                    if(current != null) current.finished();
                    current = thread;
                }
                current.add(vc);
                ph.progressProcess();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            current.finished();
            log.info("Waiting for threads to finish: ");
            for (Thread t : threads) {
                t.join();
            }
            log.info("Threads finished shuting down");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ph.finishProcess();
        
        log.info("Waiting for upload threads to finish: ");
        for(Upload u: uploads) {
            try {
                log.info("Waiting for upload: " + u.getDescription());
                u.waitForCompletion();
                log.info("Upload: " + u.getDescription() + " finished");
            } catch (AmazonClientException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("Uploads finished shuting down");
        uploadTX.shutdownNow();
    }

    public class WriterThread extends Thread {

        VariantContextWriter writer = null;
        //ConcurrentLinkedBlockingQueue<VariantContext> queue = new ConcurrentLinkedBlockingQueue<VariantContext>();
        LinkedBlockingQueue<List<VariantContext>> queue = new LinkedBlockingQueue<List<VariantContext>>(200);

        private int recordCount = 0;
        private int workBucketSize = 250;
        private volatile boolean finished = false;
        private String filePath = "";
        private String fileKey = "";
        List<VariantContext> workBucket = new ArrayList<>();

        public WriterThread(VCFHeader vcfHeader, String outputDir, String type, String chr) {
            VariantContextWriterBuilder builder = new VariantContextWriterBuilder();
            String file = type + ".vep." + chr + ".vcf.gz";
            fileKey = "variants/" + type + "/" + file;
            filePath = outputDir + "/" + file;
            log.info("Opening New file: " + filePath);
            builder.setOutputFile(filePath);
            writer = builder.build();
            writer.writeHeader(vcfHeader);
        }

        public void run() {
            while(true) {
                try {
                    List<VariantContext> vcList = queue.poll(1, TimeUnit.SECONDS);
                    if(vcList != null) {
                        for(VariantContext vc: vcList) {
                            writer.add(vc);
                            recordCount++;
                        }
                        vcList.clear();
                    } else if(finished) {
                        close();
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            log.info("Writer Thread Finished: " + filePath);
        }

        public void add(VariantContext vc) throws InterruptedException {
            workBucket.add(vc);
            if (workBucket.size() >= workBucketSize) {
                queue.put(workBucket);
                workBucket = new ArrayList<>();
            }
        }

        public void finished() throws InterruptedException {
            if(workBucket.size() > 0) {
                queue.put(workBucket);
                workBucket = new ArrayList<>();
            }
            finished = true;
        }

        private void close() throws Exception {

            log.info("Closing: " + filePath);
            writer.close();
            log.info("Wrote: " + recordCount + " records to the file: " + filePath);

            
            log.info("Uploading new file: " + filePath + ".tbi");
            PutObjectRequest req2 = new PutObjectRequest(bucket, fileKey, new File(filePath + ".tbi"));
            Upload upload2 = uploadTX.upload(req2);
            uploads.add(upload2);
            
            
            log.info("Uploading new file: " + filePath);
            S3ProgressListener progress = new S3ProgressListener();
            PutObjectRequest req1 = new PutObjectRequest(bucket, fileKey, new File(filePath));
            req1.setGeneralProgressListener(progress);
            Upload upload = uploadTX.upload(req1);
            progress.setUpload(upload);
            uploads.add(upload);
            
            upload2.waitForCompletion();
            log.info("Uploading file: " + filePath + ".tbi complete");
            upload.waitForCompletion();
            log.info("Uploading file: " + filePath + " complete");
        }

    }

}
