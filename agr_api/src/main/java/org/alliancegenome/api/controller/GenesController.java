package org.alliancegenome.api.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.alliancegenome.api.rest.interfaces.GenesRESTInterface;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.translators.tdf.GeneToTdfTranslator;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.apache.commons.collections.CollectionUtils;

@RequestScoped
public class GenesController implements GenesRESTInterface {

	@Inject
	private GeneService geneService;

	private final GeneToTdfTranslator geneTranslator = new GeneToTdfTranslator();
	
	private static GeneRepository repo = new GeneRepository();

	@Override
	public JsonResultResponse<Gene> getGenes(List<String> taxonID, Integer rows, Integer start) {
		LocalDateTime startDate = LocalDateTime.now();
		
		List<String> taxonList = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(taxonID)) {
			taxonList.addAll(taxonID);
		}
		OrthologyFilter orthologyFilter = new OrthologyFilter(null, taxonList, null);
		orthologyFilter.setRows(rows);
		orthologyFilter.setStart(start);

		List<Gene> genes = repo.getGenes(orthologyFilter);
		JsonResultResponse<Gene> response = new JsonResultResponse<>();
		response.setResults(genes);
		response.setTotal(repo.getGeneCount(orthologyFilter));
		response.calculateRequestDuration(startDate);
		return response;
	}

	@Override
	public String getGeneIDs(List<String> taxonID, Integer rows, Integer start) {
		List<String> taxonList = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(taxonID)) {
			taxonList.addAll(taxonID);
		}
		OrthologyFilter orthologyFilter = new OrthologyFilter(null, taxonList, null);
		orthologyFilter.setRows(rows);
		orthologyFilter.setStart(start);
		List<String> geneIDs = repo.getGeneIDs(orthologyFilter);
		StringJoiner joiner = new StringJoiner(",");
		geneIDs.forEach(joiner::add);
		return joiner.toString();
	}

	@Override
	public Response getIdMap(List<String> species) {
		List<Gene> geneList = geneService.getAllGenes(species);
		Response.ResponseBuilder responseBuilder = Response.ok(geneTranslator.getAllRowsSpecies(geneList));
		responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
		responseBuilder.header("Content-Disposition", "attachment; filename=\"GeneMapModId" + ".tsv\"");
		return responseBuilder.build();
	}

	@Override
	public Response getIdMapEnsembl(List<String> species) {
		List<Gene> geneList = geneService.getAllGenes(species);
		Response.ResponseBuilder responseBuilder = Response.ok(geneTranslator.getAllRowsEnsembl(geneList));
		responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
		responseBuilder.header("Content-Disposition", "attachment; filename=\"GeneMapEnsemblId" + ".tsv\"");
		return responseBuilder.build();
	}

	@Override
	public Response getIdMapNcbi(List<String> species) {
		List<Gene> geneList = geneService.getAllGenes(species);
		Response.ResponseBuilder responseBuilder = Response.ok(geneTranslator.getAllRowsNcbi(geneList));
		responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
		responseBuilder.header("Content-Disposition", "attachment; filename=\"GeneMapNcbiId" + ".tsv\"");
		return responseBuilder.build();
	}

}
