package org.alliancegenome.es.index.site.dao;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.ESDAO;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;

public class AlleleDAO extends ESDAO {

    public Map<String, Object> getById(String id) {

        try {
            GetRequest request = new GetRequest();
            request.id(id);
            request.type("allele");
            request.index(ConfigHelper.getEsIndex());
            GetResponse res = searchClient.get(request).get();
            //log.info(res);
            return res.getSource();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return null;

    }
}
