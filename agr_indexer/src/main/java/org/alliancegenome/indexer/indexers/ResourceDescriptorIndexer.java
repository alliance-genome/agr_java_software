package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.config.RestConfig;
import org.alliancegenome.core.document.ResourceDescriptorDocument;
import org.alliancegenome.core.document.ResourceDescriptorPageDocument;
import org.alliancegenome.core.es.util.ProcessDisplayHelper;
import org.alliancegenome.curation_api.interfaces.crud.ResourceDescriptorCrudInterface;
import org.alliancegenome.curation_api.model.entities.ResourceDescriptor;
import org.alliancegenome.curation_api.model.entities.ResourceDescriptorPage;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class ResourceDescriptorIndexer extends Indexer {

	private static final int FETCH_LIMIT = 10000;

	private final ResourceDescriptorCrudInterface resourceDescriptorApi = RestProxyFactory.createProxy(
		ResourceDescriptorCrudInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public ResourceDescriptorIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index(ProcessDisplayHelper display) {
		try {
			SearchResponse<ResourceDescriptor> response = resourceDescriptorApi.find(0, FETCH_LIMIT, new HashMap<>());
			List<ResourceDescriptor> all = response != null && response.getResults() != null
				? response.getResults()
				: new ArrayList<>();
			if (all.size() >= FETCH_LIMIT) {
				log.warn("Resource Descriptor fetch returned {} rows — at FETCH_LIMIT cap, results may be truncated", all.size());
			}
			List<ResourceDescriptorDocument> docs = all.stream()
				.filter(rd -> rd != null
					&& !Boolean.TRUE.equals(rd.getInternal())
					&& !Boolean.TRUE.equals(rd.getObsolete()))
				.map(ResourceDescriptorIndexer::toDocument)
				.collect(Collectors.toList());
			log.info("Indexing {} resource_descriptor documents", docs.size());
			indexDocuments(docs);
		} catch (Exception e) {
			log.error("Error while indexing resource descriptors", e);
			ExceptionCatcher.report(e);
			System.exit(-1);
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
	}

	private static ResourceDescriptorDocument toDocument(ResourceDescriptor rd) {
		ResourceDescriptorDocument document = new ResourceDescriptorDocument();
		document.setPrefix(rd.getPrefix());
		document.setName(rd.getName());
		document.setSynonyms(rd.getSynonyms());
		document.setIdExample(rd.getIdExample());
		document.setIdPattern(rd.getIdPattern());
		document.setDefaultUrlTemplate(rd.getDefaultUrlTemplate());
		if (rd.getResourcePages() != null) {
			document.setResourcePages(rd.getResourcePages().stream()
				.map(ResourceDescriptorIndexer::toPageDocument)
				.collect(Collectors.toList()));
		}
		return document;
	}

	private static ResourceDescriptorPageDocument toPageDocument(ResourceDescriptorPage page) {
		ResourceDescriptorPageDocument pageDocument = new ResourceDescriptorPageDocument();
		pageDocument.setName(page.getName());
		pageDocument.setUrlTemplate(page.getUrlTemplate());
		pageDocument.setPageDescription(page.getPageDescription());
		return pageDocument;
	}
}
