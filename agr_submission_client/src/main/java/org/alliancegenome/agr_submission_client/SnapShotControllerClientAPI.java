package org.alliancegenome.agr_submission_client;

import java.util.List;

import org.alliancegenome.agr_submission.entities.SnapShot;
import org.alliancegenome.agr_submission.interfaces.server.SnapShotControllerInterface;

import si.mazi.rescu.RestProxyFactory;

public class SnapShotControllerClientAPI implements SnapShotControllerInterface {

    private SnapShotControllerInterface api;
    
    public SnapShotControllerClientAPI() { }
    
    public SnapShotControllerClientAPI(String baseUrl) {
        api = RestProxyFactory.createProxy(SnapShotControllerInterface.class, baseUrl);
    }
    
    @Override
    public SnapShot create(SnapShot entity) {
        return api.create(entity);
    }

    @Override
    public SnapShot get(Long id) {
        return api.get(id);
    }

    @Override
    public SnapShot update(SnapShot entity) {
        return api.update(entity);
    }

    @Override
    public SnapShot delete(Long id) {
        return api.delete(id);
    }

    @Override
    public List<SnapShot> getSnapShots() {
        return api.getSnapShots();
    }

}
