package org.alliancegenome.indexer.variant;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.config.VariantConfigHelper;
import org.alliancegenome.core.es.util.IndexManager;
import org.alliancegenome.core.es.util.IndexName;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.variant.es.managers.SourceDocumentCreationManager;
import org.alliancegenome.indexer.variant.filedownload.model.DownloadFileSet;
import org.alliancegenome.indexer.variant.filedownload.process.FileDownloadManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class Main {

	private Main() {
	}

	public static void main(String[] args) {
		ConfigHelper.init();
		VariantConfigHelper.init();
		ExceptionCatcher.initialize();
		
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		boolean downloading = VariantConfigHelper.isDownloading();
		boolean creating = VariantConfigHelper.isCreating();
		boolean indexing = VariantConfigHelper.isIndexing();

		try {
			DownloadFileSet downloadSet = mapper.readValue(Main.class.getClassLoader().getResourceAsStream(VariantConfigHelper.getVariantDownloadSetFile()), DownloadFileSet.class);
			downloadSet.setDownloadPath(VariantConfigHelper.getVariantFileDownloadPath());

			if (downloading) {
				FileDownloadManager fdm = new FileDownloadManager(downloadSet);
				fdm.start();
				fdm.join();
			}

			if (creating) {
				IndexManager im = new IndexManager("variant");

				if (indexing) {
					im.startIndexes(IndexName.getVariantIndexSet());
				}

				SourceDocumentCreationManager vdm = new SourceDocumentCreationManager(downloadSet);
				vdm.start();
				vdm.join();

				if (indexing) {
					im.finishIndex(IndexName.getVariantIndexSet());
				}
			}

		} catch (Exception e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
			System.exit(-1);
		}

	}
}
