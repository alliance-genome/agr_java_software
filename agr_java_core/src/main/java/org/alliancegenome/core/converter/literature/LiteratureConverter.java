package org.alliancegenome.core.converter.literature;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.core.document.LiteratureSearchResultDocument;
import org.alliancegenome.core.document.LiteratureSummaryDocument;

public class LiteratureConverter {

	public List<LiteratureSearchResultDocument> convertToSearchResults(List<LiteratureSummaryDocument> summaryList) {
		ArrayList<LiteratureSearchResultDocument> ret = new ArrayList<LiteratureSearchResultDocument>();
		for (LiteratureSummaryDocument summaryDoc: summaryList) {
			ret.add(createDocument(summaryDoc));
		}
		return ret;
	}

	private LiteratureSearchResultDocument createDocument(LiteratureSummaryDocument document) {

		LiteratureSearchResultDocument doc = new LiteratureSearchResultDocument();
		Map<String, Object> summary = document.getLiteratureSummary();

		doc.setName((String) summary.get("title"));
		doc.setNameKey((String) summary.get("title"));

		doc.setPrimaryKey((String) summary.get("curie"));
		doc.setAbstractText((String) summary.get("abstract"));

		doc.setCitation((String) summary.get("citation"));
		doc.setShortCitation((String) summary.get("short_citation"));

		doc.setPublicationYear(getPublicationYear((String) summary.get("date_published")));

		doc.setAuthors(getAuthors(summary));
		doc.setCrossReferences(getCrossReferences(summary));

		return doc;

	}

	private String getPublicationYear(String datePublished) {
		if (datePublished == null || datePublished.length() < 4) {
			return null;
		}
		String year = datePublished.substring(0, 4);
		for (int i = 0; i < year.length(); i++) {
			if (!Character.isDigit(year.charAt(i))) {
				return null;
			}
		}
		return year;
	}

	private Set<String> getAuthors(Map<String, Object> summary) {
		List<Map<String, Object>> authors = (List<Map<String, Object>>) summary.get("authors");
		if (authors == null) {
			return null;
		}
		Set<String> ret = new LinkedHashSet<>();
		for (Map<String, Object> author : authors) {
			String name = (String) author.get("name");
			if (name != null) {
				ret.add(name);
			}
		}
		return ret.isEmpty() ? null : ret;
	}

	private Set<String> getCrossReferences(Map<String, Object> summary) {
		List<Map<String, Object>> crossReferences = (List<Map<String, Object>>) summary.get("cross_references");
		if (crossReferences == null) {
			return null;
		}
		Set<String> ret = new LinkedHashSet<>();
		for (Map<String, Object> crossReference : crossReferences) {
			if ("true".equals(crossReference.get("is_obsolete"))) {
				continue;
			}
			String curie = (String) crossReference.get("curie");
			if (curie != null) {
				ret.add(curie);
			}
		}
		return ret.isEmpty() ? null : ret;
	}

}
