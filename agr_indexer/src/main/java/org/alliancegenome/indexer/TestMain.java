package org.alliancegenome.indexer;

import java.util.HashMap;

import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.indexers.curation.interfaces.AlleleDiseaseAnnotationInterface;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import si.mazi.rescu.RestProxyFactory;

public class TestMain {

	public static void main(String[] args) throws Exception {
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		
		
		AlleleDiseaseAnnotationInterface alleleApi = RestProxyFactory.createProxy(AlleleDiseaseAnnotationInterface.class, "https://curation.alliancegenome.org/api", RestConfig.config);

		HashMap<String, Object> params = new HashMap<String, Object>();
		
		params.put("inferredGene.curie", "FB:FBgn0026379");
		
		SearchResponse<AlleleDiseaseAnnotation> resp = alleleApi.find(0, 5, params);
		
		for(AlleleDiseaseAnnotation ada: resp.getResults()) {
			String json = mapper.writeValueAsString(ada.getInferredGene());
			System.out.println(json);
			//System.out.println(ada.getInferredGene().getGeneSymbol().getDisplayText());
		}
	}

}
