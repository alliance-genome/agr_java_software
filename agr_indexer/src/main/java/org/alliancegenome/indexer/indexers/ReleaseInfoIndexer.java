package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.ReleaseInfoDocument;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.config.IndexerConfig;

public class ReleaseInfoIndexer extends Indexer {

	public ReleaseInfoIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index(ProcessDisplayHelper display) {
		ReleaseInfoDocument releaseInfoDocument = new ReleaseInfoDocument();
		releaseInfoDocument.setReleaseDate(new Date());
		releaseInfoDocument.setReleaseVersion(ConfigHelper.getAllianceRelease());
		List<ReleaseInfoDocument> releaseInfoDocumentList = new ArrayList<>();
		releaseInfoDocumentList.add(releaseInfoDocument);
		indexDocuments(releaseInfoDocumentList);
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		
	}
	
}
