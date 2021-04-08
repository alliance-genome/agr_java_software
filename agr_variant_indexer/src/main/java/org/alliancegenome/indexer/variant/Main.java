package org.alliancegenome.indexer.variant;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.filedownload.model.DownloadFileSet;
import org.alliancegenome.core.filedownload.process.FileDownloadManager;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.es.index.site.schema.VariantMapping;
import org.alliancegenome.es.index.site.schema.settings.VariantIndexSettings;
import org.alliancegenome.es.util.*;
import org.alliancegenome.indexer.variant.es.managers.*;
import org.elasticsearch.client.RestHighLevelClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class Main {

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        ConfigHelper.init();
        VariantConfigHelper.init();
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        RestHighLevelClient client = null;
        
        boolean downloading = VariantConfigHelper.isDownloading();
        boolean creating = VariantConfigHelper.isCreating();
        boolean indexing = VariantConfigHelper.isIndexing();
        
        if(indexing) client = EsClientFactory.getDefaultEsClient();
        
        try {
            // need to set VARIANTS_TO_INDEX = "HUMAN" or "MOD" to choose between which variants to index
            DownloadFileSet downloadSet = mapper.readValue(getClass().getClassLoader().getResourceAsStream(VariantConfigHelper.getDownloadSetFile()), DownloadFileSet.class);
            downloadSet.setDownloadPath(VariantConfigHelper.getVariantFileDownloadPath());

            if(downloading) {
                FileDownloadManager fdm = new FileDownloadManager(downloadSet);
                fdm.start();
                fdm.join();
            }

            if(creating) {
                IndexManager im = new IndexManager(new VariantIndexSettings(true, VariantConfigHelper.getIndexerShards()), new VariantMapping(true));

                if(indexing) SourceDocumentCreation.indexName = im.startSiteIndex();

                SourceDocumentCreationManager vdm = new SourceDocumentCreationManager(client, downloadSet);
                vdm.start();
                vdm.join();

                if(indexing) im.finishIndex();
            }

            if(indexing) client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
