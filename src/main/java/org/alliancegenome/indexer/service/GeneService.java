package org.alliancegenome.indexer.service;

import org.alliancegenome.indexer.document.GeneDocument;
import org.alliancegenome.indexer.entity.Gene;
import org.apache.log4j.Logger;

public class GeneService extends Neo4jESService<Gene, GeneDocument> {

	private Logger log = Logger.getLogger(getClass());


}
