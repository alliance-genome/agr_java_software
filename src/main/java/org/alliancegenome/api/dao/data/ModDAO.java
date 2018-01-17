package org.alliancegenome.api.dao.data;

import javax.enterprise.context.ApplicationScoped;

import org.alliancegenome.api.dao.ESDocumentDAO;
import org.alliancegenome.api.model.esdata.ModDocument;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ModDAO extends ESDocumentDAO<ModDocument> {

    private Logger log = Logger.getLogger(getClass());

    public ModDocument getModDocument(String string) {
        log.debug("Getting Mod");
        if(string != null) {
            ModDocument mod = readDocument(string, "mod");
            log.debug("Mod: " + mod);
            if(mod == null) {
                Mod m = Mod.fromString(string);
                if(m != null) {
                    mod = new ModDocument();
                    mod.setDescription(m.description);
                    mod.setName(m.name());
                    createDocumnet(mod);
                    return mod;
                } else {
                    log.debug("Mod type was null: " + mod);
                    return null;
                }
            }
            return mod;
        }

        return null;
    }

    public enum Mod {
        // Default mod if index does not contain the mods then this will be used to inject a document
        FB("Fly Base"),
        Human("Human"),
        MGD("Mouse Genome Database"),
        RGD("Rat Genome Database"),
        SGD("Saccharomyces Genome Database"),
        WB("Worm Base"),
        ZFIN("Zebrafish Information Network"),
        ;

        private String description;

        private Mod(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }

        public static Mod fromString(String string) {
            for(Mod mod: Mod.values()) {
                if(mod.name().toLowerCase().equals(string.toLowerCase())) {
                    return mod;
                }
            }
            return null;
        }
    }
}
