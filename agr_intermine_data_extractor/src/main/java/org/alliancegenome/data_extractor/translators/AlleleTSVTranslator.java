package org.alliancegenome.data_extractor.translators;

import java.io.PrintWriter;
import java.util.*;

import org.alliancegenome.core.translators.EntityTSVTranslator;
import org.alliancegenome.core.translators.ResultTSVTranslator;
import org.alliancegenome.neo4j.entity.node.Allele;

import lombok.Getter;

@Getter
public class AlleleTSVTranslator extends ResultTSVTranslator{

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
    protected List<String> mapToRow(Map<String, Object> map) {
        return Arrays.asList(
                String.valueOf(map.get("g.primaryKey")),
                String.valueOf(map.get("g.secondaryId")),
                String.valueOf(map.get("g.name")),
                String.valueOf(map.get("g.geneSynopsis"))
            );
    }

}

