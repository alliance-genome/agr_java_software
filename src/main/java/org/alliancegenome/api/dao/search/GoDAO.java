package org.alliancegenome.api.dao.search;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;

import org.alliancegenome.api.dao.ESDAO;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;

@ApplicationScoped
public class GoDAO extends ESDAO {

    // This class is going to get replaced by a call to NEO

    public Map<String, Object> getById(String id) {

        try {
            GetRequest request = new GetRequest();
            request.id(id);
            request.type("go");
            request.index(config.getEsIndex());
            GetResponse res = searchClient.get(request).get();
            //log.info(res);
            return res.getSource();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return null;

    }
}
