package org.alliancegenome.vep.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CdsSegment implements Comparable<CdsSegment> {

	private int start;
	private int end;
	private int phase;

	public CdsSegment(int start, int end, int phase) {
		this.start = start;
		this.end = end;
		this.phase = phase;
	}

	public boolean contains(int pos) {
		return pos >= start && pos <= end;
	}

	public int getLength() {
		return end - start + 1;
	}

	@Override
	public int compareTo(CdsSegment other) {
		return Integer.compare(this.start, other.start);
	}
}
