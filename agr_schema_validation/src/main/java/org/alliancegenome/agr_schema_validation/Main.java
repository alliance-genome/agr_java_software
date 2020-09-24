package org.alliancegenome.agr_schema_validation;

import java.io.File;
import java.nio.file.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {

    public static void main(String[] args) {
        
        if(args.length > 0 && args[0].length() > 0) {
            String schema_path = args[0];
            try {

                Files.walk(Paths.get(schema_path)).filter(Files::isRegularFile).forEach(path -> {
                    if(path.toString().endsWith(".json")) {

                        try {
                            //System.out.println(path);

                            File file = path.toFile();

                            JsonNode node = JsonLoader.fromFile(file);

                            JsonNode schema = node.get("$schema");

                            if(schema != null) {

                                ListProcessingReport schemaReport = (ListProcessingReport) JsonSchemaFactory.byDefault().getSyntaxValidator().validateSchema(node);

                                if(schemaReport.isSuccess()) {
                                    log.info(file + ": " + schemaReport.isSuccess());
                                } else {
                                    //log.error("Validation Failed for: " + file + " report: " + schemaReport);
                                    throw new Exception("Validation Failed for: " + file + " report: " + schemaReport);
                                }

                            } else {
                                log.info("No Validation Needed for: " + file);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }
                    }
                });


            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
            
            log.info("All Schema files successfully validated");
            
        } else {
            log.error("Please pass a schema directory for validation: ");
            System.exit(-1);
        }
    }

}
