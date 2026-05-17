package org.alliancegenome.indexer.indexers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.util.ListUtils;
import org.alliancegenome.curation_api.interfaces.document.AccessionDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.AccessionSummaryDocument;
import org.alliancegenome.core.config.RestConfig;
import org.alliancegenome.core.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.SiteMapIdDocument;

import si.mazi.rescu.RestProxyFactory;

public class SiteMapAccessionCurationIndexer extends Indexer {

	private final AccessionDocumentInterface accessionApi = RestProxyFactory.createProxy(AccessionDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public SiteMapAccessionCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index(ProcessDisplayHelper display) {
		try {

			AccessionSummaryDocument document = accessionApi.getAccessionSummary();

			Map<String, List<String>> map = document.getIdsByType();

			List<List<String>> alleleIdLists = ListUtils.partition(map.get("allele"), 15000);

			for (int i = 0; i < alleleIdLists.size(); i++) {
				SiteMapIdDocument doc = new SiteMapIdDocument();
				doc.setLastMod(ConfigHelper.getAppStart());
				String siteMapId = "allele-sitemap-" + i;
				doc.setSiteMapId(siteMapId);
				doc.setSiteMapType("allele");
				doc.setSiteMapIds(alleleIdLists.get(i));
				indexDocument(doc);
			}

			List<List<String>> geneIdLists = ListUtils.partition(map.get("gene"), 15000);

			for (int i = 0; i < geneIdLists.size(); i++) {
				SiteMapIdDocument doc = new SiteMapIdDocument();
				doc.setLastMod(ConfigHelper.getAppStart());
				String siteMapId = "gene-sitemap-" + i;
				doc.setSiteMapId(siteMapId);
				doc.setSiteMapType("gene");
				doc.setSiteMapIds(geneIdLists.get(i));
				indexDocument(doc);
			}

			List<List<String>> variantIdLists = ListUtils.partition(map.get("variant"), 15000);

			for (int i = 0; i < variantIdLists.size(); i++) {
				SiteMapIdDocument doc = new SiteMapIdDocument();
				doc.setLastMod(ConfigHelper.getAppStart());
				String siteMapId = "variant-sitemap-" + i;
				doc.setSiteMapType("variant");
				doc.setSiteMapId(siteMapId);
				doc.setSiteMapIds(variantIdLists.get(i));
				indexDocument(doc);
			}

			List<List<String>> diseaseIdLists = ListUtils.partition(map.get("disease"), 15000);

			for (int i = 0; i < diseaseIdLists.size(); i++) {
				SiteMapIdDocument doc = new SiteMapIdDocument();
				doc.setLastMod(ConfigHelper.getAppStart());
				String siteMapId = "disease-sitemap-" + i;
				doc.setSiteMapId(siteMapId);
				doc.setSiteMapType("disease");
				doc.setSiteMapIds(diseaseIdLists.get(i));
				indexDocument(doc);
			}

		} catch (Exception e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
		}

	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
	}

}
