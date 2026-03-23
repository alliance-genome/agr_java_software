package org.alliancegenome.api.controller;

import java.util.List;

import org.alliancegenome.api.rest.interfaces.VariantRESTInterface;
import org.alliancegenome.api.service.VariantService;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class VariantController implements VariantRESTInterface {

	@Inject VariantService variantService;

	@Override
	public VariantSummaryDocument getVariant(String id) {
		VariantSummaryDocument variant = variantService.getVariantById(id);
		if (variant == null) {
			RestErrorMessage message = new RestErrorMessage();
			message.setErrors(List.of("Cannot find variant with ID: " + id));
			throw new RestErrorException(message);
		}
		return variant;
	}

}
