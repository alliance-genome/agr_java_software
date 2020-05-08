package org.alliancegenome.api.service.ensembl;

import java.util.List;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.api.service.ensembl.model.EnsemblVariant;
import org.alliancegenome.api.service.ensembl.model.EnsemblVariantConsequence;
import org.alliancegenome.api.service.ensembl.model.EnsemblVariantFeature;

import si.mazi.rescu.RestProxyFactory;

@RequestScoped
public class EnsemblVariantService implements EnsemblRestAPIInterface {

    private static EnsemblRestAPIInterface api;
    
    public EnsemblVariantService() {
        /*
        ClientConfig config = new ClientConfig();
        JacksonObjectMapperFactory factory = new JacksonObjectMapperFactory() {
            
            ObjectMapper mapper;
            
            @Override
            public ObjectMapper createObjectMapper() {
                mapper = new ObjectMapper();
                return mapper;
            }
            
            @Override
            public void configureObjectMapper(ObjectMapper objectMapper) {
                mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, true);
            }
        };
        
        config.setJacksonObjectMapperFactory(factory);
        */
        
        //api = RestProxyFactory.createProxy(EnsemblRestAPIInterface.class, "https://rest.ensembl.org/", config);
        if(api == null) {
            api = RestProxyFactory.createProxy(EnsemblRestAPIInterface.class, "https://rest.ensembl.org/");
        }
    }

    @Override
    // https://rest.ensembl.org/variation/human/rs56116432?content-type=application/json
    public EnsemblVariantFeature getVariantFeatures(String id, String species, Integer genotypes) {
        return api.getVariantFeatures(id, species, genotypes);
    }

    @Override
    public List<EnsemblVariantConsequence> getVariantConsequence(String id, String species) {
        return api.getVariantConsequence(id, species);
    }

    @Override
    // https://rest.ensembl.org/overlap/id/ENSMUSG00000027168?feature=variation
    public List<EnsemblVariant> getVariants(String id, String feature) {
        return api.getVariants(id, feature);
    }
    
}