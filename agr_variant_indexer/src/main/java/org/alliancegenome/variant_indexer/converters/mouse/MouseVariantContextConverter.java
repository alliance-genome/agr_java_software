package org.alliancegenome.variant_indexer.converters.mouse;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.variant_indexer.converters.VariantContextConverter;
import org.alliancegenome.variant_indexer.converters.human.HumanVariantContextConverter;

import com.fasterxml.jackson.core.JsonProcessingException;

import htsjdk.variant.variantcontext.VariantContext;

public class MouseVariantContextConverter extends VariantContextConverter {

    HumanVariantContextConverter hc = new HumanVariantContextConverter();

    public List<String> convertVariantContext(VariantContext ctx) {

        //try {
            //String json = mapper.writeValueAsString(ctx);
            return hc.convertVariantContext(ctx);
            
            //System.out.println(list);
        //} catch (JsonProcessingException e) {
        //  e.printStackTrace();
        //}
        
        //return new ArrayList<String>();
    }

}
