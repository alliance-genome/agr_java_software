package org.alliancegenome.indexer.variant.scripts;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.io.FileUtils;


public class TestFilePathConvert {

    public static void main(String[] args) {
        String filePath = "/Volumes/Cardano/homo_sapiens_incl_consequences-chr1.vcf.gz";
        filePath = filePath.replace("vcf.gz", "json.gz");
        System.out.println(filePath);
        
        new TestFilePathConvert();
    }
    
    public TestFilePathConvert() {
        Properties props = new Properties();
        InputStream file = getClass().getClassLoader().getResourceAsStream("config.properties");
        try {
            props.load(file);
            String num = "Y";
            System.out.println(props.getProperty("ensembl.chr" + num));
            FileUtils.copyURLToFile(new URL(props.getProperty("ensembl.chr" + num)), new File("homo_sapiens_incl_consequences-chr" + num + ".vcf.gz"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

}
