package org.alliancegenome.shared.es.document.site_index;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SourceDoclet {

	private SpeciesDoclet species;
	private String url;
	private String diseaseUrl;
	private String name;

}
