package org.alliancegenome.indexer.document.searchableitem;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(includeFieldNames=true)
public class GoSearchableItemDocument extends SearchableItemDocument {

//    'go_genes': [],
//    'go_species': [],
//    'name': line['name'],
//    'description': line['def'],
//    'go_type': line['namespace'],
//    'go_synonyms': line.get('synonym'),
//    'name_key': line['name'],
//    'id': go_id,
//    'href': 'http://amigo.geneontology.org/amigo/term/' + line['id'],
//    'category': 'go'
	
	// Go Specific Fields
	private String id;
	private List<String> go_synonyms;
	private List<String> go_genes;
	private List<String> go_species;
	private String go_type;

	@JsonIgnore
	public String getDocumentId() {
		return id;
	}
}
