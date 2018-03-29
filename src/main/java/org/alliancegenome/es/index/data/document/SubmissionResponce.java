package org.alliancegenome.es.index.data.document;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties()
public class SubmissionResponce {

	@JsonInclude(value=Include.NON_NULL)
	private HashMap<String, String> fileStatus = new HashMap<>();
	private String status;

}
