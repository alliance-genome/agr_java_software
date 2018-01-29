package org.alliancegenome.shared.es.document.site_index;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AnnotationDocument extends ESDocument implements Comparable<AnnotationDocument> {
	
	private String primaryKey;
	private String category = "diseaseAnnotation";
	private String associationType;
	private SourceDoclet source;
	private GeneDocument geneDocument;
	private List<PublicationDoclet> publications;

	@JsonIgnore
	public String getDocumentId() {
		return primaryKey;
	}
	
	@Override
	@JsonIgnore
	public String getType() {
		return category;
	}

	@Override
	public int compareTo(AnnotationDocument doc) {
		return 0;
	}
}
