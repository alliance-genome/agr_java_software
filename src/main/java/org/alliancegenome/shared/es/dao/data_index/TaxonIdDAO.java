package org.alliancegenome.shared.es.dao.data_index;

import javax.annotation.PostConstruct;

import org.alliancegenome.shared.es.dao.ESDocumentDAO;
import org.alliancegenome.shared.es.document.data_index.TaxonIdDocument;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TaxonIdDAO extends ESDocumentDAO<TaxonIdDocument> {

	private Log log = LogFactory.getLog(getClass());

	public void init() {
		super.init();
		checkIndex(config.getEsDataIndex());
		checkTaxonIds();
	}

	public TaxonIdDocument getTaxonIdDocument(String string) {
		log.debug("Getting TaxonId: " + string);
		if(Mod.fromTaxonId(string) != null) {
			string = Mod.fromTaxonId(string).taxonId;
		}
		if(Mod.fromModName(string) != null) {
			string = Mod.fromModName(string).taxonId;
		}
		if(string != null) {
			return readDocument(string, "taxonid");
		}
		return null;
	}

	private void checkTaxonIds() {
		for(Mod m: Mod.values()) {
			log.debug("TaxonId: " + m.taxonId);
			TaxonIdDocument taxonId = getTaxonIdDocument(m.taxonId);
			if(taxonId == null) {
				log.debug("Creating TaxonId in ES: " + taxonId);
				taxonId = new TaxonIdDocument();
				taxonId.setDescription(m.description);
				taxonId.setModName(m.name());
				taxonId.setTaxonId(m.taxonId);
				createDocumnet(taxonId);
			}
		}
	}

	public enum Mod {
		// Default mod if index does not contain the mods then this will be used to inject a document
		FB("Fly Base", "7227"),
		Human("Human", "9606"),
		MGD("Mouse Genome Database", "10090"),
		RGD("Rat Genome Database", "10116"),
		SGD("Saccharomyces Genome Database", "4932"),
		WB("Worm Base", "6239"),
		ZFIN("Zebrafish Information Network", "7955"),
		;

		private String description;
		private String taxonId;

		private Mod(String description, String taxonId) {
			this.description = description;
			this.taxonId = taxonId;
		}

		public String getDescription() {
			return this.description;
		}
		
		public static Mod fromModName(String modName) {
			for(Mod mod: Mod.values()) {
				if(mod.name().equals(modName)) {
					return mod;
				}
			}
			return null;
		}

		public static Mod fromTaxonId(String taxonId) {
			for(Mod mod: Mod.values()) {
				if(mod.taxonId.equals(taxonId)) {
					return mod;
				}
			}
			return null;
		}
	}
}
