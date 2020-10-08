package org.alliancegenome.variant_indexer;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.site.schema.Mapping;
import org.alliancegenome.es.index.site.schema.settings.VariantIndexSettings;
import org.alliancegenome.es.util.IndexManager;
import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.es.managers.*;
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
        ConfigHelper.init();
        VariantConfigHelper.init();
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        
        boolean downloading = VariantConfigHelper.isDownloading();
        boolean creating = VariantConfigHelper.isCreating();
        
        try {

            DownloadFileSet downloadSet = mapper.readValue(getClass().getClassLoader().getResourceAsStream(VariantConfigHelper.getVariantConfigFile()), DownloadFileSet.class);
            downloadSet.setDownloadPath(VariantConfigHelper.getVariantFileDownloadPath());
            
            if(downloading) {
                FileDownloadManager fdm = new FileDownloadManager(downloadSet);
                fdm.start();
                fdm.join();
            }

            if(creating) {
                IndexManager im = new IndexManager(new VariantIndexSettings(true, VariantConfigHelper.getIndexerShards()), new Mapping(true));
                
                SourceDocumentCreation.indexName = im.startSiteIndex();
                
                SourceDocumentCreationManager vdm = new SourceDocumentCreationManager(downloadSet);
                vdm.start();
                vdm.join();
                
                im.finishIndex();
            }
            
            //mapper.writeValue(new FileWriter(new File("downloadFileSet2.yaml")), downloadSet);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
