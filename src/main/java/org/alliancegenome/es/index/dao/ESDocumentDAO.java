package org.alliancegenome.es.index.dao;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.document.ESDocument;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ESDocumentDAO<D extends ESDocument> extends ESDAO {

	private Log log = LogFactory.getLog(getClass());
	private ObjectMapper mapper = new ObjectMapper();

	public void createDocumnet(D doc) {
		log.debug("Creating new ES doc: " + doc);
		try {
			String json = mapper.writeValueAsString(doc);
			log.info("Creating Document JSON: " + json);
			IndexRequest indexRequest = new IndexRequest();
			indexRequest.index(ConfigHelper.getEsDataIndex());
			indexRequest.id(doc.getDocumentId());
			indexRequest.type(doc.getType());
			indexRequest.source(json, XContentType.JSON);
			searchClient.index(indexRequest).get();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public D readDocument(String id, String type) {
		log.debug("Going to ES for data: " + id);
		GetResponse res = null;
		try {
			GetRequest request = new GetRequest();
			request.id(id);
			request.type(type);
			request.index(ConfigHelper.getEsDataIndex());
			res = searchClient.get(request).get();

			if(res.getSourceAsString() != null) {
				return mapper.readValue(res.getSourceAsString(), (Class<D>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
			} else {
				return null;
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			log.warn("Mapping Error Result: " + res);
			log.warn("Object not found");
			log.warn(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IndexNotFoundException e) {
			log.debug("Index and Object not found");
		}

		return null;
	}

	public void updateDocument(D doc) {
		try {
			String json = mapper.writeValueAsString(doc);
			log.debug("Updating Document: " + json);
			IndexRequest indexRequest = new IndexRequest();
			indexRequest.index(ConfigHelper.getEsDataIndex());
			indexRequest.type(doc.getType());
			indexRequest.id(doc.getDocumentId());
			indexRequest.source(json, XContentType.JSON);
			searchClient.index(indexRequest).get();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	public void deleteDocument(String id) {
		// Do nothing for now.
	}
	
	public List<D> search(QueryBuilder query) {
		
		List<D> ret = new ArrayList<D>();
		Scroll s = new Scroll(new TimeValue(60000));

		SearchRequestBuilder req = searchClient.prepareSearch()
				.setScroll(s)
				.setTypes("data_file")
				.setQuery(query)
				.setIndices(ConfigHelper.getEsDataIndex())
				.setSize(1000);
		
		log.debug("Search Query: " + req);
		
		SearchResponse scrollResp = req.execute().actionGet();
		
		do {
			for (SearchHit hit : scrollResp.getHits().getHits()) {
				try {
					ret.add(mapper.readValue(hit.getSourceAsString(), (Class<D>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0]));
				} catch (JsonParseException e) {
					e.printStackTrace();
				} catch (JsonMappingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			scrollResp = searchClient.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
		} while(scrollResp.getHits().getHits().length != 0);
		return ret;
	}

}
