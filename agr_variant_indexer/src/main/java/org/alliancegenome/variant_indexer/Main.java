package org.alliancegenome.variant_indexer;

import org.alliancegenome.es.index.site.schema.settings.VariantIndexSettings;
import org.alliancegenome.es.util.IndexManager;
import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.es.ESDocumentInjector;
import org.alliancegenome.variant_indexer.es.document.VCFDocumentCreationManager;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadFileSet;
import org.alliancegenome.variant_indexer.filedownload.process.FileDownloadManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        
        VariantConfigHelper.init();
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        IndexManager im = new IndexManager(new VariantIndexSettings(true, VariantConfigHelper.getEsNumberOfShards()));
        
        try {

            DownloadFileSet downloadSet = mapper.readValue(getClass().getClassLoader().getResourceAsStream(VariantConfigHelper.getVariantConfigFile()), DownloadFileSet.class);
            downloadSet.setDownloadPath(VariantConfigHelper.getVariantFileDownloadPath());
            
            FileDownloadManager fdm = new FileDownloadManager(downloadSet);
            fdm.start();
            fdm.join();
            
            //FileDownloadFilterManager fdfm = new FileDownloadFilterManager(downloadSet);
            //fdfm.start();
            //fdfm.join();
            
            ESDocumentInjector.indexName = im.startSiteIndex();
            
            ESDocumentInjector injector = new ESDocumentInjector();
            
            VCFDocumentCreationManager vdm = new VCFDocumentCreationManager(downloadSet, injector);
            vdm.start();
            vdm.join();
            
            im.finishIndex();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
