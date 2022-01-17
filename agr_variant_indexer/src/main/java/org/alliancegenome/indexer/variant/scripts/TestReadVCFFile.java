package org.alliancegenome.indexer.variant.scripts;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.variant.converters.*;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.neo4j.entity.SpeciesType;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.*;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TestReadVCFFile {
    LinkedBlockingDeque<ArrayList<VariantContext>> vcQueue = new LinkedBlockingDeque<ArrayList<VariantContext>>(100);
    LinkedBlockingDeque<ArrayList<String>> jsonQueue = new LinkedBlockingDeque<ArrayList<String>>(1000);
    
    private String chr = "3";
    private String[] formats;
    
    public static void main(String[] args) throws Exception {
        new TestReadVCFFile();
    }
    
    public TestReadVCFFile() throws Exception {

        VCFFileReader reader = new VCFFileReader(new File("/Users/olinblodgett/Desktop/Variants/VCF/HUMAN.v2.vep.chr" + chr + ".vcf.gz"), false);
        VCFInfoHeaderLine header = reader.getFileHeader().getInfoHeaderLine("CSQ");
        formats = header.getDescription().split("Format: ")[1].split("\\|");
        
        //System.out.println(format);

        
        CloseableIterator<VariantContext> iter1 = reader.iterator();

        ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);

        List<VCFTransform> transformers = new ArrayList<>();
        
        for(int i = 0; i < 12; i++) {
            VCFTransform transformer = new VCFTransform();
            transformer.start();
            transformers.add(transformer);
        }
        
        VCFWriter write = new VCFWriter();
        write.start();
        
        ph.startProcess("Variants", 44085586);
        ArrayList<VariantContext> workQueue = new ArrayList<VariantContext>();
        
        while(iter1.hasNext()) {
            try {
                VariantContext vc = iter1.next();
                if(workQueue.size() < 1000) {
                    workQueue.add(vc);
                } else {
                    vcQueue.put(workQueue);
                    workQueue = new ArrayList<VariantContext>();
                }
                ph.progressProcess();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(workQueue.size() > 0) {
            //vcQueue.put(workQueue);
        }
        
        ph.finishProcess();
        
        log.info("Waiting for VC Queue to empty");
        while(!vcQueue.isEmpty()) {
            Thread.sleep(1000);
        }
        log.info("VC Queue Empty shutting down transformers");
        for(VCFTransform t: transformers) {
            t.interrupt();
            t.join();
        }
        log.info("Transformers shutdown");
        
        log.info("Waiting for JSON Queue to empty");
        while(!jsonQueue.isEmpty()) {
            Thread.sleep(1000);
        }
        log.info("JSON Queue Empty shuting down transformers");
        write.interrupt();
        write.join();
    }
    
    
    private class VCFTransform extends Thread {
        
        AlleleVariantSequenceConverter converter = new AlleleVariantSequenceConverter();
        private ProcessDisplayHelper ph2 = new ProcessDisplayHelper(60000);
        
        public void run() {
            ph2.startProcess("VCFTrans: ");
            while(!(Thread.currentThread().isInterrupted())) {
                try {
                    ArrayList<VariantContext> list = vcQueue.take();
                    ArrayList<String> docList = new ArrayList<String>();
                    for(VariantContext ctx: list) {
                        //List<String> docs = converter.convertVariantContext(ctx, SpeciesType.HUMAN, formats);
                        //for(String doc: docs) {
                        //  docList.add(doc);
                        //}
                        ph2.progressProcess();
                    }
                    jsonQueue.put(docList);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            ph2.finishProcess();
        }
    }
    private class VCFWriter extends Thread {
        PrintWriter writer = null;
        private ProcessDisplayHelper ph3 = new ProcessDisplayHelper(10000);
        
        public void run() {
            
            try {
                writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File("/Users/olinblodgett/Desktop/Variants/json/HUMAN.v2.vep.chr" + chr + ".vcf.json"))));
            } catch (Exception e) {
                e.printStackTrace();
            }
            ph3.startProcess("VCFWritter: ");
            while(!(Thread.currentThread().isInterrupted())) {
                try {
                    ArrayList<String> list = jsonQueue.take();
                    for(String doc: list) {
                        writer.println(doc);
                        ph3.progressProcess();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            ph3.finishProcess();
            
            writer.close();
        }
    }
}
