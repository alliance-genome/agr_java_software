package org.alliancegenome.vep.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExonModel implements Comparable<ExonModel> {

	private int start;
	private int end;
	private int ordinal;

	public ExonModel(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public boolean contains(int pos) {
		return pos >= start && pos <= end;
	}

	@Override
	public int compareTo(ExonModel other) {
		return Integer.compare(this.start, other.start);
	}
}
