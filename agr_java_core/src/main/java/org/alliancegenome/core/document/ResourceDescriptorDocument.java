package org.alliancegenome.core.document;

import java.util.List;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.core.view.PublicView;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;

@Data
@JsonView({PublicView.ResourceDescriptor.class})
public class ResourceDescriptorDocument extends ESDocument {
	{
		category = "resource_descriptor";
	}
	private String prefix;
	private String name;
	private List<String> synonyms;
	private String idExample;
	private String idPattern;
	private String defaultUrlTemplate;
	private List<ResourceDescriptorPageDocument> resourcePages;
}
