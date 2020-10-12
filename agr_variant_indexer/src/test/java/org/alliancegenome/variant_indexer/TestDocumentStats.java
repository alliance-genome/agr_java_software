package org.alliancegenome.variant_indexer;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class TestDocumentStats {

    public static void main(String[] args) throws Exception {
        SummaryStatistics stats = new SummaryStatistics();
        
        ProcessDisplayHelper ph = new ProcessDisplayHelper(2000);
        
        EmpiricalDistribution d = new EmpiricalDistribution(30);
        
        double[] array = new double[3_500_000];
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File("/Volumes/Cardano_Backup/Variants/HUMAN.v2.vep.chr7.vcf.2.json.gz")))));
        ph.startProcess("Json Reader: ");
        
        
        String line = null;
        int counter = 0;
        while((line = reader.readLine()) != null) {
            //log.info(line);
            array[counter++] = (int)(Math.log(line.length())/Math.log(1.1));
            stats.addValue(line.length());
            ph.progressProcess();
        }
        
        d.load(array);
        

        for(SummaryStatistics s: d.getBinStats()) {
            //histogram[counter++] = stats.getN();
            System.out.println(s.getMin() + "->" + s.getMax() + ": " + s.getN());
        }

        ph.finishProcess();
        
        System.out.println(stats);
        
    }
}
