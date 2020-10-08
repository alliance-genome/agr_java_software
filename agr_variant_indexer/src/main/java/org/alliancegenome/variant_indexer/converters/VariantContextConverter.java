package org.alliancegenome.variant_indexer.converters;

import java.util.List;

import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.variant_indexer.converters.human.HumanVariantContextConverter;
import org.alliancegenome.variant_indexer.converters.mouse.MouseVariantContextConverter;

import com.fasterxml.jackson.databind.*;

import htsjdk.variant.variantcontext.VariantContext;

public abstract class VariantContextConverter {

    protected ObjectMapper mapper;

    public VariantContextConverter() {
        mapper = new ObjectMapper();
    }

    public abstract List<String> convertVariantContext(VariantContext ctx, SpeciesType speciesType);

    public static VariantContextConverter getConverter(SpeciesType speciesType) {
        if(speciesType != null) {
            Class<?> clazz = VariantConverter.getConverter(speciesType);
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
        MGD(MouseVariantContextConverter.class),
        MOD(HumanVariantContextConverter.class),
        ;
        private Class<?> converterClazz;

        VariantConverter(Class<?> converterClazz) {
            this.converterClazz = converterClazz;
        }

        public Class<?> getConverterClazz() {
            return converterClazz;
        }

        public static Class<?> getConverter(SpeciesType speciesType) {
            for(VariantConverter vc: VariantConverter.values()) {
                if(vc.name().toLowerCase().equals(speciesType.getModName())) {
                    return vc.getConverterClazz();
                }else
                    return MOD.converterClazz;
            }
            return null;
        }


    }
}
