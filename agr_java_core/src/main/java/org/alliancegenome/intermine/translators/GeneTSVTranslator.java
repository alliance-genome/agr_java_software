package org.alliancegenome.intermine.translators;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.alliancegenome.core.translators.EntityTSVTranslator;
import org.alliancegenome.neo4j.entity.node.Gene;

import lombok.Getter;

@Getter
public class GeneTSVTranslator extends EntityTSVTranslator<Gene> {

    public GeneTSVTranslator(PrintWriter writer) {
        super(writer);
    }
    
    List<String> headers = Arrays.asList(
        "Name",
        "Species",
        "Symbol"
    );

    @Override
    protected List<String> entityToRow(Gene entity) {
        
        return Arrays.asList(
            entity.getName(),
            entity.getSpecies().getName(),
            entity.getSymbol()  
        );
        
    }
}
