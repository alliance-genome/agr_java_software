package org.alliancegenome.api.service.helper;

import java.util.List;

import lombok.Getter;

public class GeneDiseaseSearchHelper extends SearchHelper {

	@Getter
	private List<String> responseFields = List.of("*");

}
