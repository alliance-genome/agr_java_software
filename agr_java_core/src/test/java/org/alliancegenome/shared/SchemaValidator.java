package org.alliancegenome.shared;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public class SchemaValidator {

    public static void main(String[] args) throws Exception {

        String path = "/Users/oblod/git/agr_architecture/agr_loader/schemas";

        List<File> schemaFiles = new ArrayList<File>();

        schemaFiles.add(new File(path + "/crossReference.json"));
        schemaFiles.add(new File(path + "/dataProvider.json"));
        schemaFiles.add(new File(path + "/globalId.json"));
        schemaFiles.add(new File(path + "/metaData.json"));
        schemaFiles.add(new File(path + "/publicationRef.json"));
        schemaFiles.add(new File(path + "/synonym.json"));

        schemaFiles.add(new File(path + "/allele/alleleMetaData.json"));

        schemaFiles.add(new File(path + "/disease/additionalGeneticComponent.json"));
        schemaFiles.add(new File(path + "/disease/diseaseMetaDataDefinition.json"));
        schemaFiles.add(new File(path + "/disease/diseaseModelAnnotation.json"));
        schemaFiles.add(new File(path + "/disease/diseaseObjectRelation.json"));
        schemaFiles.add(new File(path + "/disease/evidence.json"));
        schemaFiles.add(new File(path + "/disease/experimentalConditions.json"));
        schemaFiles.add(new File(path + "/disease/modifier.json"));

        schemaFiles.add(new File(path + "/expression/uberonStageSlimTerm.json"));
        schemaFiles.add(new File(path + "/expression/uberonStructureSlimTerm.json"));
        schemaFiles.add(new File(path + "/expression/whenExpressed.json"));
        schemaFiles.add(new File(path + "/expression/whereExpressed.json"));
        schemaFiles.add(new File(path + "/expression/wildtypeExpressionMetaDataDefinition.json"));
        schemaFiles.add(new File(path + "/expression/wildtypeExpressionModelAnnotation.json"));

        schemaFiles.add(new File(path + "/gene/gene.json"));
        schemaFiles.add(new File(path + "/gene/geneMetaData.json"));
        schemaFiles.add(new File(path + "/gene/genomeLocation.json"));

        schemaFiles.add(new File(path + "/genotype/alleleInstance.json"));
        schemaFiles.add(new File(path + "/genotype/genotype.json"));
        schemaFiles.add(new File(path + "/genotype/genotypeMetaDataDefinition.json"));

        schemaFiles.add(new File(path + "/orthology/orthology.json"));
        schemaFiles.add(new File(path + "/orthology/orthologyMetaData.json"));

        schemaFiles.add(new File(path + "/termName/phenotypeMetaDataDefinition.json"));
        schemaFiles.add(new File(path + "/termName/phenotypeModelAnnotation.json"));
        schemaFiles.add(new File(path + "/termName/phenotypeTermIdentifier.json"));

        // Test Schema Syntax
        for(File schemaFile: schemaFiles) {
            System.out.print("Testing: " + schemaFile.getAbsolutePath());
            ListProcessingReport schemaReport = (ListProcessingReport) JsonSchemaFactory.byDefault().getSyntaxValidator().validateSchema(JsonLoader.fromFile(schemaFile));
            System.out.println(" -- " + (schemaReport.isSuccess() ? "success" : "failure"));
            if(!schemaReport.isSuccess()) {
                System.out.println(schemaReport);
            }
        }
        System.out.println("We are here");

        JsonSchema schemaNode = JsonSchemaFactory.byDefault().getJsonSchema(new File("/Users/oblod/git/agr_architecture/agr_loader/schemas/disease/diseaseMetaDataDefinition.json").toURI().toString());

        File dataFilePath = new File("/Users/oblod/Desktop/AGR/data/FB_1.0.0.4_disease.json");
        JsonNode jsonNode = JsonLoader.fromFile(dataFilePath);
        ProcessingReport report = schemaNode.validate(jsonNode);


        System.out.println("Validation Complete: " + report.isSuccess());

        if(!report.isSuccess()) {
            System.out.println(report);
        }

        //schemaFilePath = new File("/Users/oblod/git/agr_architecture/agr_loader/schemas/gene/geneMetaData.json");
        //schemaNode = JsonSchemaFactory.byDefault().getJsonSchema(schemaFilePath.toURI().toString());


        //schemaFilePath = new File("/Users/oblod/git/agr_architecture/agr_loader/schemas/dataProvider.json");
        //schemaNode = JsonSchemaFactory.byDefault().getJsonSchema(schemaFilePath.toURI().toString());





    }

}
