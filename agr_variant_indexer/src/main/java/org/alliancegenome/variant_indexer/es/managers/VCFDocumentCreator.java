package org.alliancegenome.variant_indexer.es.managers;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.util.parallel.ParallelGZIPOutputStream;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.converters.VariantContextConverter;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadableFile;
import org.apache.commons.io.FilenameUtils;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VCFDocumentCreator extends Thread {

    private String localGzipFilePath;
    private String localFilePath;
    private SpeciesType speciesType;

    private LinkedBlockingDeque<VariantContext> vcQueue = new LinkedBlockingDeque<VariantContext>(VariantConfigHelper.getDocumentCreatorContextQueueSize());
    private LinkedBlockingDeque<String> jsonQueue = new LinkedBlockingDeque<String>(VariantConfigHelper.getDocumentCreatorJsonQueueSize());

    public VCFDocumentCreator(DownloadableFile downloadFile, SpeciesType speciesType) {
        this.localGzipFilePath = downloadFile.getLocalGzipFilePath();
        this.speciesType = speciesType;
        this.localFilePath = FilenameUtils.removeExtension(localGzipFilePath);
    }
    
    public void run() {
        
        VCFReader reader = new VCFReader();
        reader.start();
        
        List<VCFTransformer> transformers = new ArrayList<>();
        
        for(int i = 0; i < VariantConfigHelper.getDocumentCreatorContextTransformerThreads(); i++) {
            VCFTransformer transformer = new VCFTransformer();
            transformer.start();
            transformers.add(transformer);
        }
        
        VCFJSONWriter writer = new VCFJSONWriter();
        writer.start();

        try {
            reader.join();
            log.info("Waiting for VC Queue to empty");
            while(!vcQueue.isEmpty()) {
                Thread.sleep(1000);
            }
            log.info("VC Queue Empty shuting down transformers");
            for(VCFTransformer t: transformers) {
                t.interrupt();
                t.join();
            }
            log.info("Transformers shutdown");
            log.info("Waiting for jsonQueue to empty");
            while(!jsonQueue.isEmpty()) {
                Thread.sleep(1000);
            }
            log.info("JSON Queue Empty shuting down writer");
            writer.interrupt();
            writer.join();
            log.info("JSON Writer shutdown");
            
            log.info("Threads finished: " + localGzipFilePath);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    
    private class VCFReader extends Thread {
        
        private ProcessDisplayHelper ph1 = new ProcessDisplayHelper(log, VariantConfigHelper.getDocumentCreatorDisplayInterval());
        
        public void run() {
            ph1.startProcess("VCFReader: " + localGzipFilePath);
            VCFFileReader reader = new VCFFileReader(new File(localGzipFilePath), false);
            CloseableIterator<VariantContext> iter1 = reader.iterator();
            try {
                while(iter1.hasNext()) {
                    VariantContext vc = iter1.next();
                    vcQueue.put(vc);
                    ph1.progressProcess();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            reader.close();
            ph1.finishProcess();
        }
    }
    
    private class VCFTransformer extends Thread {
        
        VariantContextConverter converter = VariantContextConverter.getConverter(speciesType);
        private ProcessDisplayHelper ph2 = new ProcessDisplayHelper(log, VariantConfigHelper.getDocumentCreatorDisplayInterval());
        
        public void run() {
            ph2.startProcess("VCFTransformer: " + speciesType.getName());
            while(!(Thread.currentThread().isInterrupted())) {
                try {
                    VariantContext ctx = vcQueue.take();
                    List<String> docs = converter.convertVariantContext(ctx, speciesType);
                    for(String doc: docs) {
                        try {
                            jsonQueue.put(doc);
                            ph2.progressProcess("VCQueue: " + vcQueue.size() + " JsonQueue: " + jsonQueue.size());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            ph2.finishProcess();
        }
    }
    
    private class VCFJSONWriter extends Thread {
        private ProcessDisplayHelper ph3 = new ProcessDisplayHelper(log, VariantConfigHelper.getDocumentCreatorDisplayInterval());
        private BufferedOutputStream out = null;
        
        private int fileCounter = 0;
        private int fileLineCount = 0;
        private boolean skip = false;
        
        public void run() {
            ph3.startProcess("VCFJSONWriter: " + speciesType.getName());

            while(!(Thread.currentThread().isInterrupted())) {
                try {
                    updateFileCounter();
                    String doc = jsonQueue.take();
                    try {
                        if(!skip) out.write((doc + "\n").getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ph3.progressProcess("VCQueue: " + vcQueue.size() + " JsonQueue: " + jsonQueue.size());
                } catch (InterruptedException e) {
                    if(out != null) {
                        try {
                            out.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                    Thread.currentThread().interrupt();
                }
            }
            ph3.finishProcess();
        }
        
        private void updateFileCounter() {
            if(out == null && fileLineCount == 0) {
                try {
                    File file = new File(localFilePath + "." + fileCounter + ".json.gz");
                    fileCounter++;
                    if(file.exists()) {
                        skip = true;
                        log.info("Skipping File: " + file.getAbsolutePath());
                    } else {
                        out = new BufferedOutputStream(new ParallelGZIPOutputStream(new FileOutputStream(file)));
                        skip = false;
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if(fileLineCount == 3_500_000) {
                    try {
                        if(out != null) {
                            out.close();
                            out = null;
                        }
                        
                        File file = new File(localFilePath + "." + fileCounter + ".json.gz");
                        fileCounter++;
                        if(file.exists()) {
                            skip = true;
                            log.info("Skipping File: " + file.getAbsolutePath());
                        } else {
                            out = new BufferedOutputStream(new ParallelGZIPOutputStream(new FileOutputStream(file)));
                            skip = false;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    fileLineCount = 0;
                    return;
                }
            }
            fileLineCount++;
        }
        
    }

}
