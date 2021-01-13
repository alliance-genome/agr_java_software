package org.alliancegenome.core.variant.converters;

import java.util.List;

import org.alliancegenome.core.variant.converters.human.HumanVariantContextConverter;
import org.alliancegenome.es.variant.model.VariantDocument;
import org.alliancegenome.neo4j.entity.SpeciesType;

import htsjdk.variant.variantcontext.VariantContext;

public abstract class VariantContextConverter {

    public abstract List<VariantDocument> convertVariantContext(VariantContext ctx, SpeciesType speciesType, String[] header);

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
