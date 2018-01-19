package org.alliancegenome.api.dao.data;

import javax.enterprise.context.ApplicationScoped;

import org.alliancegenome.api.dao.ESDocumentDAO;
import org.alliancegenome.api.model.esdata.TaxonIdDocument;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TaxonIdDAO extends ESDocumentDAO<TaxonIdDocument> {

    private Logger log = Logger.getLogger(getClass());

    public TaxonIdDocument getTaxonIdDocument(String string) {
        log.debug("Getting TaxonId");
        if(string != null) {
            TaxonIdDocument taxonid = readDocument(string, "taxonid");
            log.debug("TaxonId: " + taxonid);
            if(taxonid == null) {
                Mod m = Mod.fromTaxonId(string);
                if(m != null) {
                    taxonid = new TaxonIdDocument();
                    taxonid.setDescription(m.description);
                    taxonid.setName(m.name());
                    createDocumnet(taxonid);
                    return taxonid;
                } else {
                    log.debug("TaxonId was null or not found for: " + taxonid);
                    return null;
                }
            }
            return taxonid;
        }

        return null;
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
