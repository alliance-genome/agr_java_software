package org.alliancegenome.variant_indexer;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.util.ProcessDisplayHelper;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.*;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.transfer.*;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.*;
import htsjdk.variant.vcf.*;
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
        String outputDir = "/Volumes/Cardano_Backup/Variants";

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



        //TransferManager tx = TransferManagerBuilder.standard().withS3Client(s3Client).build();
        //List<Upload> uploads = new ArrayList<>();
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

        //log.info("Waiting for uploads to finish: ");
        //      

        //      for(Upload u: uploads) {
        //          try {
        //              u.waitForCompletion();
        //          } catch (AmazonClientException | InterruptedException e) {
        //              e.printStackTrace();
        //          }
        //      }
        //log.info("Uploads finished shuting down");
        //tx.shutdownNow();
        ph.finishProcess();
    }

    public class WriterThread extends Thread {

        VariantContextWriter writer = null;
        //ConcurrentLinkedBlockingQueue<VariantContext> queue = new ConcurrentLinkedBlockingQueue<VariantContext>();
        LinkedBlockingQueue<List<VariantContext>> queue = new LinkedBlockingQueue<List<VariantContext>>(200);

        private int workBucketSize = 250;
        private volatile boolean finished = false;
        private String filePath = "";
        List<VariantContext> workBucket = new ArrayList<>();

        public WriterThread(VCFHeader vcfHeader, String outputDir, String type, String chr) {
            VariantContextWriterBuilder builder = new VariantContextWriterBuilder();
            filePath = outputDir + "/" + type + ".vep." + chr + ".vcf.gz";
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
                        }
                        vcList.clear();
                    } else if(finished) {
                        close();
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
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

        private void close() {
            if(writer != null) {
                log.info("Closing: " + filePath);
                writer.close();
                //log.info("Uploading new file: " + outputDir + "/" + type + ".vep." + chr + ".vcf.gz");
                //Upload upload = tx.upload(bucket, type + "/" + type + ".vep." + chr + ".vcf.gz", new File(outputDir + "/" + type + ".vep." + chr + ".vcf.gz"));
                //uploads.add(upload);
            }
        }

    }
}
