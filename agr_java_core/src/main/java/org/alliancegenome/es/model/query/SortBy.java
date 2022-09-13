package org.alliancegenome.es.model.query;

import java.util.StringJoiner;

public enum SortBy {
	DISEASE("disease"),
	SPECIES("species"),
	GENE("gene"),
	DEFAULT("default"),;
	private String name;

	SortBy(String name) {
		this.name = name;
	}

	public static SortBy getSortBy(String name) {
		if (name == null)
			return DEFAULT;
		for (SortBy sort : values()) {
			if (sort.name.equals(name))
				return sort;
		}
		return null;
	}

	public static String getAllValues() {
		StringJoiner values = new StringJoiner(",");
		for (SortBy sorting : values())
			values.add(sorting.name);
		return values.toString();
	}
}
