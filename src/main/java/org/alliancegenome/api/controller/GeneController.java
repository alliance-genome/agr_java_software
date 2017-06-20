package org.alliancegenome.api.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.api.rest.GeneRESTInterface;
import org.alliancegenome.api.service.GeneService;

import io.swagger.annotations.Api;

@Path("/gene")
@Api()
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class GeneController implements GeneRESTInterface {

	@Inject
	private GeneService geneService;

	@GET
	public String getTopCDs() {
		//geneService.message();
		JsonArrayBuilder array = Json.createArrayBuilder();
		List<Integer> randomCDs = getRandomNumbers();
		for (Integer randomCD : randomCDs) {
			array.add(Json.createObjectBuilder().add("id", randomCD));
		}
		return array.build().toString();
	}

	private List<Integer> getRandomNumbers() {
		List<Integer> randomCDs = new ArrayList<>();
		Random r = new Random();
		randomCDs.add(r.nextInt(100) + 1101);
		randomCDs.add(r.nextInt(100) + 1101);
		randomCDs.add(r.nextInt(100) + 1101);
		randomCDs.add(r.nextInt(100) + 1101);
		randomCDs.add(r.nextInt(100) + 1101);

		return randomCDs;
	}
}
