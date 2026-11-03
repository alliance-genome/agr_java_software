package org.alliancegenome.api.controller;

import java.util.List;

import org.alliancegenome.core.document.ResourceDescriptorDocument;
import org.alliancegenome.api.rest.interfaces.ResourceDescriptorRESTInterface;
import org.alliancegenome.api.service.ResourceDescriptorService;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class ResourceDescriptorController implements ResourceDescriptorRESTInterface {

	@Inject ResourceDescriptorService resourceDescriptorService;

	@Override
	public List<ResourceDescriptorDocument> getResourceDescriptors() {
		return resourceDescriptorService.getResourceDescriptors();
	}

}
