package org.alliancegenome.agr_elasticsearch_util.commands;

import java.util.ArrayList;
import java.util.HashMap;

import org.alliancegenome.shared.es.dao.data_index.DataTypeDAO;
import org.alliancegenome.shared.es.document.data_index.DataTypeDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataIndexCommand extends Command implements CommandInterface {

	private DataTypeDAO dataTypeDAO = new DataTypeDAO();
	private Logger log = LogManager.getLogger(getClass());
	
	public DataIndexCommand(ArrayList<String> args) {
		super(args);
	}

	@Override
	public void printHelp() {


	}

	@Override
	public void execute() {
		//dataindex addschema BGI 1.0.0.0 /gene/basicGeneInfoFile.json
		
		String command = args.remove(0);

		if(command.equals("addschema")) {
			String dataType = args.remove(0);
			String schemaVersion = args.remove(0);
			String path = args.remove(0);
			DataTypeDocument dtd = dataTypeDAO.getDataType(dataType);
			if(dtd != null) {
				String version = dtd.getSchemaFiles().get(schemaVersion);
				if(version == null) {
					dtd.getSchemaFiles().put(schemaVersion, path);
					dataTypeDAO.updateDocument(dtd);
					log.info("Document: " + dtd);
				} else {
					log.error("Schema Already exist use updateSchema instead");
				}
				
			} else {
				log.error("Data Type not found: " + dataType);
			}
			
		} else if(command.equals("updateschema")) {
			String dataType = args.remove(0);
			String schemaVersion = args.remove(0);
			String path = args.remove(0);
			DataTypeDocument dtd = dataTypeDAO.getDataType(dataType);
			if(dtd != null) {
				dtd.getSchemaFiles().put(schemaVersion, path);
				dataTypeDAO.updateDocument(dtd);
				log.info("Document: " + dtd);
			} else {
				log.error("Data Type not found: " + dataType);
			}
		} else if(command.equals("deleteschema")) {
			String dataType = args.remove(0);
			String schemaVersion = args.remove(0);
			String path = args.remove(0);
			DataTypeDocument dtd = dataTypeDAO.getDataType(dataType);
			
			if(dtd != null) {
				dtd.getSchemaFiles().remove(schemaVersion);
				//dtd.setSchemaFiles(new HashMap<>(dtd.getSchemaFiles()));
				dataTypeDAO.updateDocument(dtd);
				log.info("Document: " + dtd);
			} else {
				log.error("Data Type not found: " + dataType);
			}
		} else {
			printHelp();
		}
	}

}
