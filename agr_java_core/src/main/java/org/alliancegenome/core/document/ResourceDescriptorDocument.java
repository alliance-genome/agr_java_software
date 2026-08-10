package org.alliancegenome.core.document;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.ResourceDescriptor;
import org.alliancegenome.core.view.PublicView;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({ "category", "resourceDescriptor" })
@JsonView({PublicView.ResourceDescriptor.class})
public class ResourceDescriptorDocument extends ESDocument {
	{
		category = "resource_descriptor";
	}
	private ResourceDescriptor resourceDescriptor;
}
