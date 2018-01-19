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
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@RequestScoped
public class MetaDataController extends BaseController implements MetaDataRESTInterface {

    @Inject
    private ConfigHelper config;

    @Inject
    private MetaDataService metaDataService;

    private Logger log = Logger.getLogger(getClass());

    @Override
    public MetaDataDocument getMetaData() {
        MetaDataDocument data = new MetaDataDocument();
        data.setDebug(String.valueOf(config.getDebug()));
        data.setEsHost(config.getEsHost());
        data.setEsPort(String.valueOf(config.getEsPort()));
        return data;
    }

    @Override
    public SubmissionResponce submitData(String api_access_token, MultipartFormDataInput input) {
        SubmissionResponce res = new SubmissionResponce();

        Map<String, List<InputPart>> form = input.getFormDataMap();
        boolean success = true;

        for(String key: form.keySet()) {
            if(authenticate(api_access_token)) {
                InputPart inputPart = form.get(key).get(0);
                try {
                    metaDataService.submitData(key, inputPart.getBodyAsString());
                    res.getFileStatus().put(key, "success");
                } catch (GenericException | IOException e) {
                    log.error(e.getMessage());
                    res.getFileStatus().put(key, e.getMessage());
                    //e.printStackTrace();
                    success = false;
                }
            } else {
                res.getFileStatus().put(key, "Authentication Failure: Please check your api_access_token");
                success = false;
            }
        }
        if(success) {
            res.setStatus("success");
        } else {
            res.setStatus("failed");
        }
        return res;

    }

    @Override
    public SubmissionResponce validateData(String api_access_token, MultipartFormDataInput input) {
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
