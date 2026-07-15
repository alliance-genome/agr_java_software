package org.alliancegenome.indexer.service;

import java.util.HashMap;
import java.util.Map;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.config.RestConfig;
import org.alliancegenome.curation_api.model.entities.ResourceDescriptor;
import org.alliancegenome.curation_api.model.entities.ResourceDescriptorPage;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.interfaces.ResourceDescriptorPageInterface;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

/**
 * Resolves curie → URL template 
 */
@Slf4j
public class ResourceDescriptorService {

	private static final int PAGE_FETCH_LIMIT = 10000;

	private final ResourceDescriptorPageInterface resourceDescriptorPageApi = RestProxyFactory.createProxy(ResourceDescriptorPageInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private volatile Map<String, Map<String, String>> pagesByPrefix;
	private volatile Map<String, String> defaultsByPrefix;

	/**
	 * Eagerly load the descriptor cache. Call from an indexer's index() method
	 * before initiateThreading() so worker threads observe a populated map.
	 * Idempotent and thread-safe via double-checked locking.
	 */
	public void warm() {
		if (pagesByPrefix == null) {
			synchronized (this) {
				if (pagesByPrefix == null) {
					load();
				}
			}
		}
	}

	public String resolveUrlTemplate(String curie) {
		return resolveUrlTemplate(curie, null);
	}

	public String resolveUrlTemplate(String curie, String pageName) {
		if (curie == null) {
			return null;
		}
		String trimmed = curie.trim();
		int colon = trimmed.indexOf(':');
		if (colon <= 0) {
			return null;
		}

		String prefix = trimmed.substring(0, colon);
		// Defensive lazy-load in case warm() was skipped; idempotent under DCL above.
		warm();

		if (pageName != null) {
			Map<String, String> pages = pagesByPrefix.get(prefix);
			if (pages != null) {
				String pageTemplate = pages.get(pageName);
				if (pageTemplate != null) {
					return pageTemplate;
				}
			}
		}
		return defaultsByPrefix.get(prefix);
	}

	private void load() {
		SearchResponse<ResourceDescriptorPage> response;
		try {
			response = resourceDescriptorPageApi.findForPublic(0, PAGE_FETCH_LIMIT, new HashMap<>());
		} catch (Exception e) {
			log.error("Failed to load ResourceDescriptorPages from curation API at " + ConfigHelper.getCurationApiUrl() + " — reference URL resolution will be unavailable", e);
			throw new RuntimeException("ResourceDescriptorService bootstrap failed", e);
		}
		Map<String, Map<String, String>> pageMap = new HashMap<>();
		Map<String, String> defaultMap = new HashMap<>();
		// Two-pass: real prefixes first so synonyms cannot shadow them when the same
		// alias is both a real prefix on descriptor A and a synonym on descriptor B.
		for (ResourceDescriptorPage page : response.getResults()) {
			ResourceDescriptor rd = page.getResourceDescriptor();
			if (rd == null || rd.getPrefix() == null || page.getName() == null) {
				continue;
			}
			registerPageForPrefix(pageMap, defaultMap, rd.getPrefix(), page, rd);
		}
		for (ResourceDescriptorPage page : response.getResults()) {
			ResourceDescriptor rd = page.getResourceDescriptor();
			if (rd == null || rd.getSynonyms() == null || page.getName() == null) {
				continue;
			}
			for (String synonym : rd.getSynonyms()) {
				if (synonym != null && !synonym.isEmpty()) {
					registerPageForPrefix(pageMap, defaultMap, synonym, page, rd);
				}
			}
		}
		if (response.getResults().size() >= PAGE_FETCH_LIMIT) {
			log.warn("ResourceDescriptorService load hit PAGE_FETCH_LIMIT={} — response may be truncated; consider paging", PAGE_FETCH_LIMIT);
		}
		log.info("ResourceDescriptorService loaded {} pages across {} prefixes", response.getResults().size(), pageMap.size());
		pagesByPrefix = pageMap;
		defaultsByPrefix = defaultMap;
	}

	private static void registerPageForPrefix(Map<String, Map<String, String>> pageMap, Map<String, String> defaultMap, String prefix, ResourceDescriptorPage page, ResourceDescriptor rd) {
		pageMap.computeIfAbsent(prefix, k -> new HashMap<>()).putIfAbsent(page.getName(), page.getUrlTemplate());
		if (rd.getDefaultUrlTemplate() != null) {
			defaultMap.putIfAbsent(prefix, rd.getDefaultUrlTemplate());
		}
	}
}
