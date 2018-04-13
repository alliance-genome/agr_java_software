package org.alliancegenome.es.index.site.doclet;

import org.alliancegenome.es.index.doclet.Doclet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SourceDoclet extends Doclet {

	private SpeciesDoclet species;
	private String url;
	private String diseaseUrl;
	private String name;

}
