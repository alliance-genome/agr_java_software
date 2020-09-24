package org.alliancegenome.data_extractor.translators;

import java.io.PrintWriter;
import java.util.*;

import org.alliancegenome.core.translators.EntityTSVTranslator;
import org.alliancegenome.neo4j.entity.node.Allele;

public class AlleleTSVTranslator extends EntityTSVTranslator<Allele> {

    public AlleleTSVTranslator(PrintWriter writer) {
        super(writer);
    }

    @Override
    protected List<String> getHeaders() {
        return Arrays.asList(
                "Id",
                "Gene",
                "Species",
                "Symbol"
            );
    }

    @Override
    protected List<String> entityToRow(Allele entity) {
        return Arrays.asList(
                entity.getPrimaryKey(),
                entity.getGene().getSymbol(),
                entity.getSpecies().getType().getTaxonID(),
                entity.getSymbolText()
            );
    }

}

