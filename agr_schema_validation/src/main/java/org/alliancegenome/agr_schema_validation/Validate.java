package org.alliancegenome.agr_schema_validation;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Validate {

	public static void main(String[] args) throws Exception {

		File file = new File(args[0]);

		JsonSchema schemaNode = JsonSchemaFactory.byDefault().getJsonSchema(file.toURI().toString());
		JsonNode jsonNode = JsonLoader.fromFile(new File(args[1]));

		ProcessingReport report = schemaNode.validate(jsonNode);

		if(!report.isSuccess()) {
			for(ProcessingMessage message: report) {
				log.info(message.getMessage());
			}
		}
		log.info("Validation Complete: " + report.isSuccess());


	}

}
