package org.alliancegenome.data_extractor.translators;

import java.io.PrintWriter;
import java.util.*;

import org.alliancegenome.core.translators.EntityTSVTranslator;
import org.alliancegenome.core.translators.ResultTSVTranslator;
import org.alliancegenome.neo4j.entity.node.Gene;

import lombok.Getter;

@Getter
public class GeneTSVTranslator extends ResultTSVTranslator {

    public GeneTSVTranslator(PrintWriter writer) {
        super(writer);
    }

    @Override
    protected List<String> getHeaders() {
        return Arrays.asList(
                "Id",
                "Secondary Id",
                "Name",
                "MOD Description",
                "Automated Description",
                "Species",
                "Chromosome",
                "Start",
                "End",
                "Strand",
                "SoTerm"
            );
    }

    @Override
    protected List<String> mapToRow(Map<String, Object> map) {

        return Arrays.asList(
            String.valueOf(map.get("g.primaryKey")),
            String.valueOf(map.get("g.secondaryId")),
            String.valueOf(map.get("g.name")),
            String.valueOf(map.get("g.geneSynopsis")),
            String.valueOf(map.get("g.automatedGeneSynopsis")),
            String.valueOf(map.get("s.primaryKey")),
            String.valueOf(map.get("gl.chromosome")),
            String.valueOf(map.get("gl.start")),
            String.valueOf(map.get("gl.end")),
            String.valueOf(map.get("gl.strand")),
            String.valueOf(map.get("so.name"))

        );
    }

}
