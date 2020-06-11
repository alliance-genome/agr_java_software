package org.alliancegenome.variant_indexer.converters;

import java.util.List;

import org.alliancegenome.variant_indexer.converters.human.HumanVariantContextConverter;
import org.alliancegenome.variant_indexer.converters.mouse.MouseVariantContextConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

import htsjdk.variant.variantcontext.VariantContext;

public abstract class VariantContextConverter {

    protected ObjectMapper mapper;
    
    public VariantContextConverter() {
        mapper = new ObjectMapper();
    }

    public abstract List<String> convertVariantContext(VariantContext ctx);

    public static VariantContextConverter getConverter(String converterName) {
        if(converterName == null) {
        
        } else {
            Class<?> clazz = VariantConverter.getConverter(converterName);
            try {
                return (VariantContextConverter) clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private enum VariantConverter {
        HUMAN(HumanVariantContextConverter.class),
        MOUSE(MouseVariantContextConverter.class),
        ;

        private Class<?> converterClazz;
        
        VariantConverter(Class<?> converterClazz) {
            this.converterClazz = converterClazz;
        }
        
        public Class<?> getConverterClazz() {
            return converterClazz;
        }

        public static Class<?> getConverter(String converterName) {
            for(VariantConverter vc: VariantConverter.values()) {
                if(vc.name().toLowerCase().equals(converterName)) {
                    return vc.getConverterClazz();
                }
            }
            return null;
        }
        
        
    }
}
