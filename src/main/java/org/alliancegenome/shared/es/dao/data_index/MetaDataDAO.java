package org.alliancegenome.shared.es.dao.data_index;

import org.alliancegenome.shared.config.ConfigHelper;
import org.alliancegenome.shared.es.dao.ESDocumentDAO;
import org.alliancegenome.shared.es.document.data_index.MetaDataDocument;


public class MetaDataDAO extends ESDocumentDAO<MetaDataDocument> {
	
	public MetaDataDAO() {
		checkIndex(ConfigHelper.getEsDataIndex());
	}

}
