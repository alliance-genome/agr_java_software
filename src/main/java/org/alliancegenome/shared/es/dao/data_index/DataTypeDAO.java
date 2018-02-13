package org.alliancegenome.shared.es.dao.data_index;

import org.alliancegenome.shared.config.ConfigHelper;
import org.alliancegenome.shared.es.dao.ESDocumentDAO;
import org.alliancegenome.shared.es.document.data_index.DataTypeDocument;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class DataTypeDAO extends ESDocumentDAO<DataTypeDocument> {

	private Log log = LogFactory.getLog(getClass());
	
	public DataTypeDAO() {
		checkIndex(ConfigHelper.getEsDataIndex());
		checkDataTypes();
	}

	// Null -> return null
	// Invalid data type -> null
	// Data Type not in ES but in Valid in ENUM -> data type plus inject document
	public DataTypeDocument getDataType(String string) {
		log.debug("Getting Data Type: " + string);
		if(string != null) {
			return readDocument(string, "data_type");
		}
		return null;
	}

	private void checkDataTypes() {
		for(DataType dataType: DataType.values()) {
			log.debug("Data Type: " + dataType);
			DataTypeDocument dt = getDataType(dataType.name());
			if(dt == null) {
				log.debug("Creating Datatype in ES: " + dataType);
				dt = new DataTypeDocument();
				dt.setDescription(dataType.description);
				dt.setFileExtension(dataType.fileExtension);
				dt.setTaxonIdRequired(dataType.taxonIdRequired);
				dt.setName(dataType.name());
				dt.setValidationRequired(dataType.validationRequired);
				createDocumnet(dt);
			}
		}
	}

	public enum DataType {
		// Default data type if index does not contain the data type then this will be used to inject a document
		BGI("Basic Gene Information", "json", true, true),
		DOA("Disease Ontology Annotations", "json", true, true),
		ORTHO("Orthology", "json", true, true),
		FEATURE("Feature Information", "json", true, true),

		// No schema required for these but will still stick them in the correct schema directory
		GOA("Gene Ontology Annotations", "gaf", true, false),
		GFF("Gene Features File", "gff", true, false),

		DO("Disease Ontology", "obo", false, false),
		GO("Gene Ontology", "obo", false, false),
		SO("Sequence Ontology", "obo", false, false),
		;

		private boolean taxonIdRequired;
		private boolean validationRequired;
		private String fileExtension;
		private String description;

		private DataType(String description, String fileExtension, boolean taxonIdRequired, boolean validationRequired) {
			this.description = description;
			this.fileExtension = fileExtension;
			this.taxonIdRequired = taxonIdRequired;
			this.validationRequired = validationRequired;
		}

		public static DataType fromString(String string) {
			for(DataType dt: DataType.values()) {
				if(dt.name().toLowerCase().equals(string.toLowerCase())) {
					return dt;
				}
			}
			return null;
		}

	}
}
