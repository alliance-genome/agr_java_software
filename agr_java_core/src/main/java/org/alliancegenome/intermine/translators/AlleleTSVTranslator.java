package org.alliancegenome.intermine.translators;

import java.io.PrintWriter;
import java.util.List;

import org.alliancegenome.core.translators.EntityTSVTranslator;
import org.alliancegenome.neo4j.entity.node.Allele;

public class AlleleTSVTranslator extends EntityTSVTranslator<Allele> {

    public AlleleTSVTranslator(PrintWriter writer) {
        super(writer);
    }

    @Override
    protected List<String> getHeaders() {
        return null;
    }

    @Override
    protected List<String> entityToRow(Allele entity) {
        return null;
    }

}
