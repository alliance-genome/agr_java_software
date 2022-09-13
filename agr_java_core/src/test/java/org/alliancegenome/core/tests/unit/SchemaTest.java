package org.alliancegenome.core.tests.unit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.alliancegenome.core.tests.SchemaFileVisitor;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public class SchemaTest {

	@Test
	public void checkVersion() {
		String schemaVersion = "0.1.0.0";

		String[] array = schemaVersion.split("\\.");


		int out = Integer.parseInt(array[0] + array[1] + array[2] + array[3]);
		out--;

		String a = (out / 1000) + "";
		out = out % 1000;
		String b = (out / 100) + "";
		out = out % 100;
		String c = (out / 10) + "";
		out = out % 10;
		String d = out + "";

		//System.out.println(a + b + c + d);
		Assert.assertEquals((a + b + c + d), "0099");
	}
	
	@Test
	@Ignore
	public void testAllSchemaFiles() {
		SchemaFileVisitor schemaVisitor = new SchemaFileVisitor();
		// TODO this path needs to get passed in for better testing
		Path p = Paths.get("/Users/balrog/git/agr_loader/schemas");
		try {
			Files.walkFileTree(p, schemaVisitor);
			//System.out.println(schemaVisitor.getFileList());
			for(Path path: schemaVisitor.getFileList()) {
				//System.out.println("Loading Schema Files: " + path);
				
				System.out.print("Validating Schema: " + path.toFile().getAbsolutePath());
				ListProcessingReport schemaReport = (ListProcessingReport) JsonSchemaFactory.byDefault().getSyntaxValidator().validateSchema(JsonLoader.fromFile(path.toFile()));
				System.out.println(" -- " + (schemaReport.isSuccess() ? "success" : "failure"));
				if(!schemaReport.isSuccess()) {
					System.out.println(schemaReport);
					Assert.fail();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

}
