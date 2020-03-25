package org.alliancegenome.api.service;

import java.util.Map;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class EntityService {

    private EntityService() {} // Cannot be instantiated needs to be @Injected
    
    public Map<String, Object> getById(String id) {
        // Needs to be Implemented in Neo
        return null;
    }

}
