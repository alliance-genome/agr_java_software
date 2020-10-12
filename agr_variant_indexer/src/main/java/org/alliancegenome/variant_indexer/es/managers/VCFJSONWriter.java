package org.alliancegenome.variant_indexer.es.managers;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.util.parallel.ParallelGZIPOutputStream;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.util.CountedBufferedOutputStream;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class VCFJSONWriter extends Thread {

    private ProcessDisplayHelper ph = new ProcessDisplayHelper(VariantConfigHelper.getDocumentCreatorDisplayInterval());

    private LinkedBlockingDeque<String> jsonQueue;
    private HashMap<Integer, VCFJSONFileWriter> fileWritters = new HashMap<Integer, VCFJSONFileWriter>();

    private String downloadPath;

    public VCFJSONWriter(LinkedBlockingDeque<String> jsonQueue, String downloadPath) {
        this.jsonQueue = jsonQueue;
        //this.downloadPath = downloadPath;
        this.downloadPath = "/Users/olinblodgett/Desktop/Variants";
    }

    public void run() {
        ph.startProcess("VCFJSONWriter: ");

        while(!(Thread.currentThread().isInterrupted())) {
            try {
                String doc = jsonQueue.take();
                try {
                    VCFJSONFileWriter writer = getOutputFileWriter((int)(Math.log(doc.length()) / Math.log(1.1)));
                    writer.addLine(doc);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ph.progressProcess("JsonQueue: " + jsonQueue.size());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        ph.finishProcess();

        for(VCFJSONFileWriter writer: fileWritters.values()) {
            try {
                writer.waitForClose();
                writer.interrupt();
                writer.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private VCFJSONFileWriter getOutputFileWriter(int size) throws Exception {
        VCFJSONFileWriter fileWriter = fileWritters.get(size);
        if(fileWriter == null) {
            fileWriter = new VCFJSONFileWriter(size);
            fileWriter.start();
            fileWritters.put(size, fileWriter);
        }
        return fileWriter;
    }

    private class VCFJSONFileWriter extends Thread {
        private LinkedBlockingDeque<String> lineQueue = new LinkedBlockingDeque<String>(25000);
        private CountedBufferedOutputStream out = null;
        private int fileCount = 0;
        private int docSize;

        public VCFJSONFileWriter(int docSize) {
            this.docSize = docSize;
        }

        private CountedBufferedOutputStream newOutFile(int docSize) throws Exception {
            if(out != null) {
                out.flush();
                out.close();
            }
            int buffersize = Math.max((int)(Math.pow(1.1, docSize) * 5), 8192 * 10);
            String fileName = downloadPath + "/DataFile." + docSize + "." + fileCount + ".json.gz";
            fileCount++;
            return new CountedBufferedOutputStream(new ParallelGZIPOutputStream(new FileOutputStream(fileName)), buffersize);
        }

        public void run() {
            ph.startProcess("VCFJSONFileWriter: ");

            while(!(Thread.currentThread().isInterrupted())) {
                try {
                    String doc = lineQueue.take();
                    try {
                        if(out == null || out.getByteCount() >= 21_474_836_480L) out = newOutFile(docSize);
                        out.write((doc + "\n").getBytes());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ph.progressProcess("JsonQueue: " + jsonQueue.size());
                } catch (InterruptedException e) {
                    try {
                        out.flush();
                        out.close();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    Thread.currentThread().interrupt();
                }
            }
            ph.finishProcess();
        }

        public void addLine(String line) {
            try {
                lineQueue.put(line);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void waitForClose() throws InterruptedException {
            while(lineQueue.size() > 0) {
                Thread.sleep(1000);
            }
        }
    }

}
