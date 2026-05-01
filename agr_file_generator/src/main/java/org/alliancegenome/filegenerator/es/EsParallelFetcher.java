package org.alliancegenome.filegenerator.es;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.rest.RestConfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class EsParallelFetcher {

	private static final String SCROLL_DURATION = "5m";

	private final EsRestInterface es;
	private final String index;
	private final List<String> categories;
	private final ObjectMapper om = new ObjectMapper();

	public EsParallelFetcher(String index, List<String> categories) {
		this.es = RestProxyFactory.createProxy(EsRestInterface.class, buildEsUrl(), RestConfig.config);
		this.index = index;
		this.categories = categories;
	}

	/**
	 * Run an ES `_count` against the same {@code categories.terms} filter. Returns the total
	 * number of docs the scroll will see (so callers can size a progress bar). Returns 0 if
	 * the count call fails — better to show no progress than to fail the whole generator.
	 */
	public long count() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("query", Map.of("terms", Map.of("category.keyword", categories)));
		try {
			Map<String, Object> resp = es.count(index, body);
			Object c = resp.get("count");
			if (c instanceof Number) {
				return ((Number) c).longValue();
			}
		} catch (Exception e) {
			log.warn("count() failed on {} for {}: {}", index, categories, e.getMessage());
		}
		return 0;
	}

	public void forEach(int threadCount, int bufferSize, Consumer<JsonNode> consumer) {
		forEach(threadCount, bufferSize, null, consumer);
	}

	public void forEach(int threadCount, int bufferSize, List<String> sourceIncludes, Consumer<JsonNode> consumer) {
		Map<String, Object> baseBody = buildBaseBody(bufferSize, sourceIncludes);
		log.debug("Starting sliced scroll on {} for categories {} with {} slices, bufferSize={}, sourceIncludes={}",
				index, categories, threadCount, bufferSize, sourceIncludes);

		List<Thread> threads = new ArrayList<>();
		for (int i = 0; i < threadCount; i++) {
			final int sliceId = i;
			Thread t = new Thread(() -> worker(sliceId, threadCount, baseBody, consumer), "es-slice-" + i);
			threads.add(t);
			t.start();
		}
		for (Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Interrupted while waiting for slice threads", e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void worker(int sliceId, int sliceMax, Map<String, Object> baseBody, Consumer<JsonNode> consumer) {
		Map<String, Object> initialBody = new LinkedHashMap<>(baseBody);
		if (sliceMax > 1) {
			initialBody.put("slice", Map.of("id", sliceId, "max", sliceMax));
		}

		String scrollId = null;
		long sliceCount = 0;
		try {
			Map<String, Object> resp = es.startScroll(index, SCROLL_DURATION, initialBody);
			while (resp != null) {
				scrollId = (String) resp.get("_scroll_id");
				Map<String, Object> hitsMap = (Map<String, Object>) resp.get("hits");
				if (hitsMap == null) {
					break;
				}
				List<Map<String, Object>> hits = (List<Map<String, Object>>) hitsMap.get("hits");
				if (hits == null || hits.isEmpty()) {
					break;
				}

				for (Map<String, Object> hit : hits) {
					Object src = hit.get("_source");
					JsonNode node = om.valueToTree(src);
					consumer.accept(node);
				}
				sliceCount += hits.size();

				resp = es.continueScroll(Map.of("scroll", SCROLL_DURATION, "scroll_id", scrollId));
			}
			log.debug("Slice {}/{} done: {} hits", sliceId, sliceMax, sliceCount);
		} catch (Exception e) {
			log.error("Slice {} failed: {}", sliceId, e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	private Map<String, Object> buildBaseBody(int bufferSize, List<String> sourceIncludes) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("size", bufferSize);
		body.put("query", Map.of("terms", Map.of("category.keyword", categories)));
		body.put("sort", List.of("_doc"));
		if (sourceIncludes != null && !sourceIncludes.isEmpty()) {
			body.put("_source", sourceIncludes);
		}
		return body;
	}

	private static String buildEsUrl() {
		String host = ConfigHelper.getEsHost();
		int port = ConfigHelper.getEsPort();
		String h = host;
		if (h.contains(",")) {
			h = h.split(",")[0];
		}
		if (h.contains(":")) {
			return "http://" + h;
		}
		return "http://" + h + ":" + port;
	}
}
