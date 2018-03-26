package org.alliancegenome.es.index.data.dao;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.dao.ESDocumentDAO;
import org.alliancegenome.es.index.data.document.MetaDataDocument;


public class MetaDataDAO extends ESDocumentDAO<MetaDataDocument> {
	
	public MetaDataDAO() {
		checkIndex(ConfigHelper.getEsDataIndex());
	}

}
