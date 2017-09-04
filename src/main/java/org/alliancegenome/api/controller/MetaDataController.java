package org.alliancegenome.api.controller;

import javax.inject.Inject;

import org.alliancegenome.api.config.ConfigHelper;
import org.alliancegenome.api.model.MetaData;
import org.alliancegenome.api.rest.interfaces.MetaDataRESTInterface;

public class MetaDataController implements MetaDataRESTInterface {

	@Inject
	private ConfigHelper config;
	
	@Override
	public MetaData getMetaData() {
		MetaData data = new MetaData();
		data.setDebug(String.valueOf(config.getDebug()));
		data.setEsHost(config.getEsHost());
		data.setEsIndex(config.getEsIndex());
		data.setEsPort(String.valueOf(config.getEsPort()));
		return data;
	}

}
