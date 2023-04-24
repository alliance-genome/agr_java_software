package org.alliancegenome.api.tests.integration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.EntitySummary;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.repository.InteractionRepository;
import org.alliancegenome.neo4j.view.View;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InteractionsIT {

	public static InteractionRepository repo = new InteractionRepository();
	
	@Inject
	private GeneService geneService;

	public static void main(String[] args) throws Exception {

		//List<InteractionGeneJoin> list = repo.getInteractions("MGI:99604");
		//List<InteractionGeneJoin> list = repo.getInteractions("MGI:97490");
		//List<InteractionGeneJoin> list = repo.getInteractions("WB:WBGene00003912");
		//MGI:103150
		List<InteractionGeneJoin> list = repo.getInteractions("MGI:103150"); // 135, 116
		//List<InteractionGeneJoin> list = repo.getInteractions("HGNC:11998");


		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
		mapper.setSerializationInclusion(Include.NON_NULL);

		String json = mapper.writerWithView(View.Interaction.class).writeValueAsString(list);

		log.info("Json: " + json);
		log.info("Count: " + list.size());
		for (InteractionGeneJoin join : list) {
			log.info("Join: " + join);
		}
	}

	@Test
	public void getInteractionSummary() {
		EntitySummary summary = geneService.getInteractionSummary("MGI:109583");
		assertNotNull(summary);
		assertThat(summary.getNumberOfAnnotations(), greaterThanOrEqualTo(34L));
		assertThat(summary.getNumberOfEntities(), greaterThanOrEqualTo(18L));

		summary = geneService.getInteractionSummary("HGNC:4601");
		assertNotNull(summary);
		assertThat(summary.getNumberOfAnnotations(), greaterThanOrEqualTo(154L));
		assertThat(summary.getNumberOfEntities(), greaterThanOrEqualTo(84L));

	}

	@Test
	public void getInteractionFieldValues() {
		JsonResultResponse<InteractionGeneJoin> response = geneService.getInteractions("MGI:109583", new Pagination());
		assertNotNull(response);

		final Map<String, List<String>> distinctFieldValues = response.getDistinctFieldValues();
		assertNotNull(distinctFieldValues);
		assertThat(3, greaterThanOrEqualTo(distinctFieldValues.size()));

	}
}
