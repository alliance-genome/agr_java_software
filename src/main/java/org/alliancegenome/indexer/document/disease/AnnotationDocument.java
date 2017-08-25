package org.alliancegenome.indexer.document.disease;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.indexer.document.ESDocument;
import org.alliancegenome.indexer.document.gene.GeneDocument;

import java.util.List;

@Getter @Setter
public class AnnotationDocument extends ESDocument implements Comparable<AnnotationDocument> {
	
	private String primaryKey;
	private GeneDocument geneDocument;
	private List<PublicationDocument> publications;

	@JsonIgnore
	public String getDocumentId() {
		return primaryKey;
	}

	@Override
	public int compareTo(AnnotationDocument doc) {
		return 0;
	}
}
