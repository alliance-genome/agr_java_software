package org.alliancegenome.api.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.config.ConfigHelper;
import org.alliancegenome.api.exceptions.GenericException;
import org.alliancegenome.api.model.esdata.MetaDataDocument;
import org.alliancegenome.api.model.esdata.SubmissionResponce;
import org.alliancegenome.api.rest.interfaces.MetaDataRESTInterface;
import org.alliancegenome.api.service.MetaDataService;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@RequestScoped
public class MetaDataController implements MetaDataRESTInterface {

    @Inject
    private ConfigHelper config;

    @Inject
    private MetaDataService metaDataService;

    @Override
    public MetaDataDocument getMetaData() {
        MetaDataDocument data = new MetaDataDocument();
        data.setDebug(String.valueOf(config.getDebug()));
        data.setEsHost(config.getEsHost());
        data.setEsPort(String.valueOf(config.getEsPort()));
        return data;
    }

    @Override
    public SubmissionResponce submitData(MultipartFormDataInput input) {
        Map<String, List<InputPart>> form = input.getFormDataMap();
        SubmissionResponce res = new SubmissionResponce();
        for(String key: form.keySet()) {
            InputPart inputPart = form.get(key).get(0);
            try {
                metaDataService.submitData(key, inputPart.getBodyAsString());
            } catch (Exception e) {
                e.printStackTrace();
                res.setError(e.getMessage());
                res.setStatus("failed");
                return res;
            }
        }
        res.setStatus("success");
        return res;
    }

    @Override
    public SubmissionResponce validateData(MultipartFormDataInput input) {
        Map<String, List<InputPart>> form = input.getFormDataMap();
        for(String key: form.keySet()) {
            InputPart inputPart = form.get(key).get(0);
            try {
                boolean isValid = metaDataService.validateData(key, inputPart.getBodyAsString());
                if(isValid) {

                } else {

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new SubmissionResponce();
    }

}
