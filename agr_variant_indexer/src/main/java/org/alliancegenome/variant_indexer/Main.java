package org.alliancegenome.variant_indexer;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.download.FileDownloadManager;
import org.alliancegenome.variant_indexer.download.model.DownloadFileSet;
import org.alliancegenome.variant_indexer.vcf.VCFDocumentCreationManager;
import org.alliancegenome.variant_indexer.vcf.VCFDocumentIndexerManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        VariantConfigHelper.init();

        try {
            DownloadFileSet downloadSet = mapper.readValue(getClass().getClassLoader().getResourceAsStream(VariantConfigHelper.getVariantConfigFile()), DownloadFileSet.class);
            downloadSet.setDownloadPath(VariantConfigHelper.getVariantFileDownloadPath());
            
            FileDownloadManager dm = new FileDownloadManager(downloadSet);
            dm.start();
            dm.join();
            
            VCFDocumentCreationManager vdm = new VCFDocumentCreationManager(downloadSet);
            vdm.start();
            vdm.join();
            
            VCFDocumentIndexerManager vdim = new VCFDocumentIndexerManager(downloadSet);
            vdim.start();
            vdim.join();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
