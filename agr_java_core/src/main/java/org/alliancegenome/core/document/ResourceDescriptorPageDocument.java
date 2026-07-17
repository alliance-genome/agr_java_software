package org.alliancegenome.core.document;

import org.alliancegenome.core.view.PublicView;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;

// Embedded page entry of a ResourceDescriptorDocument. Deliberately a plain POJO (not an ESDocument)
// mapped from the curation ResourceDescriptorPage entity, to drop that entity's @ManyToOne back-reference
// to its parent (serialization cycle) and its audit fields.
@Data
@JsonView({PublicView.ResourceDescriptor.class})
public class ResourceDescriptorPageDocument {
	private String name;
	private String urlTemplate;
	private String pageDescription;
}
