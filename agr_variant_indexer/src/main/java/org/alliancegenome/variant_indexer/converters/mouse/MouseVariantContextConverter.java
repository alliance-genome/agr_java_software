package org.alliancegenome.variant_indexer.converters.mouse;

import java.util.List;

import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.variant_indexer.converters.VariantContextConverter;
import org.alliancegenome.variant_indexer.converters.human.HumanVariantContextConverter;

import htsjdk.variant.variantcontext.VariantContext;

public class MouseVariantContextConverter extends VariantContextConverter {

    HumanVariantContextConverter hc = new HumanVariantContextConverter();

    public List<String> convertVariantContext(VariantContext ctx, SpeciesType speciesType) {

        //try {
            //String json = mapper.writeValueAsString(ctx);
            return hc.convertVariantContext(ctx, speciesType);
            
            //System.out.println(list);
        //} catch (JsonProcessingException e) {
        //  e.printStackTrace();
        //}
        
        //return new ArrayList<String>();
    }

}
