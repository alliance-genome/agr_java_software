package org.alliancegenome.api.dao.data;

import javax.enterprise.context.ApplicationScoped;

import org.alliancegenome.api.dao.ESDocumentDAO;
import org.alliancegenome.api.model.esdata.SchemaDocument;

@ApplicationScoped
public class SchemaDAO extends ESDocumentDAO<SchemaDocument> {

}
