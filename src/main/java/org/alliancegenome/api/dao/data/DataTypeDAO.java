package org.alliancegenome.api.dao.data;

import javax.enterprise.context.ApplicationScoped;

import org.alliancegenome.api.dao.ESDocumentDAO;
import org.alliancegenome.api.model.esdata.DataTypeDocument;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DataTypeDAO extends ESDocumentDAO<DataTypeDocument> {

    private Logger log = Logger.getLogger(getClass());

    // Null -> return null
    // Invalid data type -> null
    // Data Type not in ES but in Valid in ENUM -> data type plus inject document
    public DataTypeDocument getDataType(String string) {
        log.debug("Getting Data Type");
        if(string != null) {
            DataTypeDocument dataType = readDocument(string, "data_type");
            log.debug("Data Type: " + dataType);
            if(dataType == null) {
                DataType dt = DataType.fromString(string);
                if(dt != null) {
                    dataType = new DataTypeDocument();
                    dataType.setDescription(dt.description);
                    dataType.setFileExtension(dt.fileExtension);
                    dataType.setModRequired(dt.modRequired);
                    dataType.setName(dt.name());
                    dataType.setValidationRequired(dt.validationRequired);
                    createDocumnet(dataType);
                    return dataType;
                } else {
                    log.debug("Github Data Type was null: " + dataType);
                    return null;
                }
            }
            return dataType;
        }

        return null;
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

        private boolean modRequired;
        private boolean validationRequired;
        private String fileExtension;
        private String description;

        private DataType(String description, String fileExtension, boolean modRequired, boolean validationRequired) {
            this.description = description;
            this.fileExtension = fileExtension;
            this.modRequired = modRequired;
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
