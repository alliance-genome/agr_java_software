package org.alliancegenome.variant_indexer;

import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.download.model.DownloadFileSet;
import org.alliancegenome.variant_indexer.es.ESDocumentInjector;
import org.alliancegenome.variant_indexer.vcf.FileDownloadFilterManager;
import org.alliancegenome.variant_indexer.vcf.FileDownloadManager;
import org.alliancegenome.variant_indexer.vcf.VCFDocumentCreationManager;

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
            
            FileDownloadManager fdm = new FileDownloadManager(downloadSet);
            fdm.start();
            fdm.join();
            
            FileDownloadFilterManager fdfm = new FileDownloadFilterManager(downloadSet);
            fdfm.start();
            fdfm.join();

            ESDocumentInjector edi = new ESDocumentInjector();
            edi.createIndex();
            
            VCFDocumentCreationManager vdm = new VCFDocumentCreationManager(downloadSet);
            vdm.start();
            vdm.join();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
