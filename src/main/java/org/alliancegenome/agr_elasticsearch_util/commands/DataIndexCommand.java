package org.alliancegenome.agr_elasticsearch_util.commands;

import java.util.ArrayList;

import org.alliancegenome.es.index.data.dao.MetaDataDAO;
import org.alliancegenome.es.index.data.document.DataTypeDoclet;
import org.alliancegenome.es.index.data.document.MetaDataDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataIndexCommand extends Command implements CommandInterface {

	private MetaDataDAO metaDataDAO = new MetaDataDAO();
	private Logger log = LogManager.getLogger(getClass());

	public DataIndexCommand(ArrayList<String> args) {
		super(args);
	}

	@Override
	public void printHelp() {
		System.out.println("dataindex addschema <version> -- this adds a schema version to the schema list");
		System.out.println("dataindex deleteschema <version> -- this removes a schema version to the schema list");

		System.out.println("dataindex addschemafile <type> <version> <filePath> -- this adds a filePath to a particulr version of the schmea for a single datatype");
		System.out.println("dataindex updateschemafile <type> <version> <filePath> -- this updates a filePath to a particulr version of the schmea for a single datatype");
		System.out.println("dataindex deleteschemafile <type> <version> <filePath> -- this deletes a filePath to a particulr version of the schmea for a single datatype");
		
		System.out.println("dataindex addreleaseschema <release> <schema> -- this adds a schema to a release, release to schema is M to 1 relationship");
		System.out.println("dataindex updatereleaseschema <release> <schema> -- this updates a release to a new schema, release to schema is M to 1 relationship");
		System.out.println("dataindex deletereleaseschema <release> <schema> -- this removes the release from the schema map, release to schema is M to 1 relationship");
	}

	@Override
	public void execute() {
		//dataindex addschema BGI 1.0.0.0 /gene/basicGeneInfoFile.json
		if(args.size() > 0) {
			String command = args.remove(0);
			MetaDataDocument metaData = metaDataDAO.readDocument("meta_data_id", "meta_data");
			
			if(command.equals("addschemafile")) {
				String dataType = args.remove(0);
				String schemaVersion = args.remove(0);
				String path = args.remove(0);
				DataTypeDoclet dtd = metaData.getDataTypes().get(dataType);
				if(dtd != null) {
					String version = dtd.getSchemaFiles().get(schemaVersion);
					if(version == null) {
						dtd.getSchemaFiles().put(schemaVersion, path);
						metaDataDAO.updateDocument(metaData);
						log.debug("Doclet: " + dtd);
					} else {
						log.error("Schema Already exist use updateSchema instead");
					}

				} else {
					log.error("Data Type not found: " + dataType);
				}
				
			} else if(command.equals("addschema")) {
				String schemaVersion = args.remove(0);
				if(!metaData.getSchemas().contains(schemaVersion)) {
					metaData.getSchemas().add(schemaVersion);
					metaDataDAO.updateDocument(metaData);
				} else {
					log.error("Schema Already exists use deleteschema instead: " + schemaVersion);
				}
			} else if(command.equals("deleteschema")) {
				String schemaVersion = args.remove(0);
				if(metaData.getSchemas().contains(schemaVersion)) {
					metaData.getSchemas().remove(schemaVersion);
					metaDataDAO.updateDocument(metaData);
				} else {
					log.error("Schema not found use addschema instead: " + schemaVersion);
				}
			} else if(command.equals("addreleaseschema")) {
				String releaseVersion = args.remove(0);
				String schemaVersion = args.remove(0);
				if(!metaData.getReleaseSchemaMap().containsKey(releaseVersion)) {
					metaData.getReleaseSchemaMap().put(releaseVersion, schemaVersion);
					metaDataDAO.updateDocument(metaData);
				} else {
					log.error("Release Already associated with Schema use updatereleaseschema instead: " + releaseVersion + " " + schemaVersion);
				}
			} else if(command.equals("updatereleaseschema")) {
				String releaseVersion = args.remove(0);
				String schemaVersion = args.remove(0);
				if(metaData.getReleaseSchemaMap().containsKey(releaseVersion)) {
					metaData.getReleaseSchemaMap().put(releaseVersion, schemaVersion);
					metaDataDAO.updateDocument(metaData);
				} else {
					log.error("Release doesn't exist please use addreleaseschema instead: " + releaseVersion + " " + schemaVersion);
				}
			} else if(command.equals("deletereleaseschema")) {
				String releaseVersion = args.remove(0);
				String schemaVersion = args.remove(0);
				if(metaData.getReleaseSchemaMap().containsKey(releaseVersion)) {
					if(metaData.getReleaseSchemaMap().get(releaseVersion).equals(schemaVersion)) {
						metaData.getReleaseSchemaMap().remove(releaseVersion);
						metaDataDAO.updateDocument(metaData);
					} else {
						log.error("Release Schema mismatch please use updatereleaseschema instead: " + releaseVersion + " " + schemaVersion);
					}
				} else {
					log.error("Release Doesn't exist please use addreleaseschema instead: " + releaseVersion + " " + schemaVersion);
				}
			} else if(command.equals("updateschemafile")) {
				String dataType = args.remove(0);
				String schemaVersion = args.remove(0);
				String path = args.remove(0);
				DataTypeDoclet dtd = metaData.getDataTypes().get(dataType);
				if(dtd != null) {
					dtd.getSchemaFiles().put(schemaVersion, path);
					metaDataDAO.updateDocument(metaData);
					log.debug("Doclet: " + dtd);
				} else {
					log.error("Data Type not found: " + dataType);
				}
			} else if(command.equals("deleteschemafile")) {
				String dataType = args.remove(0);
				String schemaVersion = args.remove(0);
				//String path = args.remove(0);
				
				DataTypeDoclet dtd = metaData.getDataTypes().get(dataType);

				if(dtd != null) {
					dtd.getSchemaFiles().remove(schemaVersion);
					//dtd.setSchemaFiles(new HashMap<>(dtd.getSchemaFiles()));
					metaDataDAO.updateDocument(metaData);
					log.debug("Doclet: " + dtd);
				} else {
					log.error("Data Type not found: " + dataType);
				}
			} else {
				printHelp();
			}
		} else {
			printHelp();
		}
	}

}
