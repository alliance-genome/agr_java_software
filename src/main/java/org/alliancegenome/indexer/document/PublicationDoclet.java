package org.alliancegenome.indexer.document;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicationDoclet {

	private String primaryKey;
	private String pubMedId;
	private String pubMedUrl;
	private String pubModId;
	private String pubModUrl;
    private List<String> evidenceCodes;

}
