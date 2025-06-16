package org.alliancegenome.indexer.indexers;

import java.lang.reflect.UndeclaredThrowableException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.curation.LitDocument;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.indexer.indexers.curation.interfaces.LiteratureElasticSearchInterface;

import si.mazi.rescu.HttpStatusIOException;
import si.mazi.rescu.RestProxyFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import groovyjarjarpicocli.CommandLine.Help.Ansi.IStyle;

import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LitIndexer extends Indexer {
	private static final Logger logger = LoggerFactory.getLogger(LitIndexer.class);
	private HashSet<String> curieSet = new HashSet<>();
	private String indexName = ConfigHelper.getBlueTeamESIndex();
	
	public LitIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
		
	}

	private LiteratureElasticSearchInterface literatureESApi = RestProxyFactory.createProxy(LiteratureElasticSearchInterface.class, ConfigHelper.getBueTeamESUrl());


	//with multiple thread  
	@Override
	protected void index() {
		//String indexName= ConfigHelper.getBlueTeamESIndex();
		System.out.println("indexName in index:" + indexName);
		Map<String, Object> countObject = literatureESApi.count(indexName);		
		try {
		
			int totalPages = (int)countObject.get("count") / indexerConfig.getBufferSize();			
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();	
			for (int i = 0; i <= totalPages; i++) {
				int from = i* indexerConfig.getBufferSize();
				queue.add(String.valueOf(from));
			}
			
			System.out.println("total pages:" + totalPages);	
			initiateThreading(queue);
		
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
 
	
	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
	    ObjectMapper mapper = new ObjectMapper();
	    //String indexName = ConfigHelper.getBlueTeamESIndex();
	    System.out.println("indexName in startSingleThread:"+indexName);
	    while (!queue.isEmpty()) {
	        String page = null;
	        try {
	            page = queue.takeFirst();
	        } catch (InterruptedException e) {
	            Thread.currentThread().interrupt();
	            return;
	        }

	        int maxRetries = 5;
	        int attempt = 0;
	        long waitTimeMs = 1000;

	        while (attempt < maxRetries) {
	            try {
	                Map<String, Object> object = literatureESApi.search(
	                	indexName,
	                    Integer.parseInt(page),
	                    indexerConfig.getBufferSize()
	                );
                    System.out.println("page/from:"+ page + " size: " + indexerConfig.getBufferSize());
	                Map<String, Object> hitsMap = (Map<String, Object>) object.get("hits");
	                List<Map<String, Object>> hits = (List<Map<String, Object>>) hitsMap.get("hits");

	                List<LitDocument> list = new ArrayList<>();

	                for (Map<String, Object> map : hits) {
	                    Map<String, Object> sourceMap = (Map<String, Object>) map.get("_source");
	                    ObjectNode sourceJson = mapper.convertValue(sourceMap, ObjectNode.class);

	                    // Copy "category" to "literature_category"
	                    if (sourceJson.has("category")) {
	                        sourceJson.set("alliance_reference_type", sourceJson.get("category"));
	                    }
	                    
	                    String curie=sourceJson.get("curie").toString();
		                if (curieSet.contains(curie)) {
		                    System.out.println("Duplicate curie found: " + curie + " total:"+ curieSet.size());
		                    System.exit(1);
		                } else {
		                    curieSet.add(curie);
		                }

	                    // Normalize each date field if present
	                    normalizeAndPutDate(sourceJson, "date_last_modified_in_pubmed");
	                    normalizeAndPutDate(sourceJson, "date_published");
	                    normalizeAndPutDate(sourceJson, "date_arrived_in_pubmed");
	                    normalizeAndPutDate(sourceJson, "date_published_start");
	                    normalizeAndPutDate(sourceJson, "date_published_end");

	                    LitDocument doc = new LitDocument();
	                    doc.setObject(sourceJson);
	                    list.add(doc);
	                }

	                indexDocuments(list);
	                break; // success, break out of retry loop

	            } catch (UndeclaredThrowableException e) {
	            	Throwable cause = e.getUndeclaredThrowable();
	                if (cause instanceof HttpStatusIOException) {
	                    HttpStatusIOException httpEx = (HttpStatusIOException) cause;
	                    if (httpEx.getHttpStatusCode() == 429) {
	                        System.out.println("Received 429. Retrying page " + page + " in " + waitTimeMs + "ms (attempt " + (attempt + 1) + ")");
	                        try {
	                            Thread.sleep(waitTimeMs);
	                        } catch (InterruptedException ie) {
	                            Thread.currentThread().interrupt();
	                            return;
	                        }
	                        waitTimeMs *= 2;
	                        attempt++;
	                    } else {
	                        throw new RuntimeException("HTTP error fetching page " + page, httpEx);
	                    }
	                } else {
	                    throw new RuntimeException("Unexpected undeclared throwable for page " + page, e);
	                }
	            } catch (Exception e) {
	                throw new RuntimeException("Unhandled error for page " + page, e);
	            }
	        }

	        if (attempt >= maxRetries) {
	            System.err.println("Max retries exceeded for page " + page + ". Skipping.");
	        }
	    }
	}

	// Helper method
	private void normalizeAndPutDate(ObjectNode json, String fieldName) {
	    if (json.hasNonNull(fieldName)) {
	        String normalized = normalizeDate(json.get(fieldName).asText());
	        json.put(fieldName, normalized);
	    }
	}

	private String normalizeDate(String raw) {
        final String DEFAULT_DATE = "1900-01-01";

        if (raw == null || raw.trim().isEmpty() || raw.trim().equalsIgnoreCase("none")) {
            return DEFAULT_DATE;
        }

        raw = raw.trim();

        try {
            // Case: yyyy-M-d or yyyy-MM-dd
            if (raw.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
                LocalDate parsed = LocalDate.parse(raw, DateTimeFormatter.ofPattern("yyyy-M-d"));
                return parsed.format(DateTimeFormatter.ISO_LOCAL_DATE);
            }

            // Case: yyyy.M.d
            if (raw.matches("^\\d{4}\\.\\d{1,2}\\.\\d{1,2}$")) {
                LocalDate parsed = LocalDate.parse(raw, DateTimeFormatter.ofPattern("yyyy.M.d"));
                return parsed.format(DateTimeFormatter.ISO_LOCAL_DATE);
            }

            // Case: yyyy MMM dd
            Pattern pattern1 = Pattern.compile("(\\d{4})\\s+([A-Za-z]{3})\\s*(\\d{1,2})?");
            Matcher matcher1 = pattern1.matcher(raw);
            if (matcher1.find()) {
                String year = matcher1.group(1);
                String month = matcher1.group(2);
                String day = matcher1.group(3) != null ? matcher1.group(3) : "1";
                SimpleDateFormat input = new SimpleDateFormat("yyyy MMM dd", Locale.ENGLISH);
                SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd");
                return output.format(input.parse(year + " " + month + " " + day));
            }

            // Case: yyyy only
            if (raw.matches("^\\d{4}$")) {
                return raw + "-01-01";
            }

        } catch (Exception e) {
            System.err.println("Failed to normalize date: '" + raw + "'");
        }

        return DEFAULT_DATE;
    }

}
