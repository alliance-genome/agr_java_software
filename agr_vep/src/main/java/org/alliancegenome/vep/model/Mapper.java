package org.alliancegenome.vep.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Port of Bio::EnsEMBL::Mapper (1257 lines).
 * Generic coordinate mapping between two coordinate systems.
 *
 * Stores pairs of coordinates (from↔to) and maps positions between them.
 * Handles both normal mappings and insertions (start = end + 1).
 */
public class Mapper {

	/** A mapped coordinate result (Bio::EnsEMBL::Mapper::Coordinate). */
	public static class Coordinate {
		public int start;
		public int end;
		public int strand;
		public String id;

		public Coordinate(String id, int start, int end, int strand) {
			this.id = id;
			this.start = start;
			this.end = end;
			this.strand = strand;
		}
	}

	/** A gap in the mapping (Bio::EnsEMBL::Mapper::Gap). */
	public static class Gap {
		public int start;
		public int end;

		public Gap(int start, int end) {
			this.start = start;
			this.end = end;
		}
	}

	/** A mapping result — either a Coordinate or a Gap. */
	public static class Result {
		public final Coordinate coordinate;
		public final Gap gap;

		private Result(Coordinate c, Gap g) {
			this.coordinate = c;
			this.gap = g;
		}

		public static Result coord(Coordinate c) { return new Result(c, null); }
		public static Result gap(Gap g) { return new Result(null, g); }
		public boolean isGap() { return gap != null; }
		public boolean isCoordinate() { return coordinate != null; }
	}

	/** A coordinate pair (one from each coordinate system). */
	private static class Pair {
		CoordRange from;
		CoordRange to;
		int ori; // relative orientation: 1 or -1

		Pair(CoordRange from, CoordRange to, int ori) {
			this.from = from;
			this.to = to;
			this.ori = ori;
		}
	}

	private static class CoordRange {
		String id;
		int start;
		int end;

		CoordRange(String id, int start, int end) {
			this.id = id;
			this.start = start;
			this.end = end;
		}
	}

	private final String fromName;
	private final String toName;
	private final List<Pair> pairs = new ArrayList<>();
	private boolean sorted = false;

	public Mapper(String from, String to) {
		this.fromName = from;
		this.toName = to;
	}

	/**
	 * VEP Mapper::add_map_coordinates (line 665-720).
	 * Add a coordinate pair mapping.
	 */
	public void addMapCoordinates(String fromId, int fromStart, int fromEnd, int strand,
			String toId, int toStart, int toEnd) {
		CoordRange from = new CoordRange(fromId, fromStart, fromEnd);
		CoordRange to = new CoordRange(toId, toStart, toEnd);
		int ori = strand; // +1 or -1
		pairs.add(new Pair(from, to, ori));
		sorted = false;
	}

	/**
	 * VEP Mapper::map_coordinates (line 253-458).
	 * Map coordinates from one system to the other.
	 *
	 * @param id ID in the source coordinate system
	 * @param start Start position
	 * @param end End position
	 * @param strand Strand (+1 or -1)
	 * @param type Source coordinate system name
	 * @return List of Result objects (Coordinate or Gap)
	 */
	public List<Result> mapCoordinates(String id, int start, int end, int strand, String type) {
		// VEP line 267-269: special case for insertions (start = end + 1)
		if (start == end + 1) {
			return mapInsert(id, start, end, strand, type);
		}

		if (!sorted) sort();

		boolean isFromTo = type.equals(fromName);
		List<Pair> relevantPairs = getRelevantPairs(id, isFromTo);
		List<Result> results = new ArrayList<>();

		int currentStart = start;
		Pair lastUsedPair = null;

		for (Pair pair : relevantPairs) {
			CoordRange selfCoord = isFromTo ? pair.from : pair.to;
			CoordRange targetCoord = isFromTo ? pair.to : pair.from;

			// Check overlap
			if (selfCoord.end < currentStart || selfCoord.start > end) continue;

			// VEP line 344-351: gap before this pair
			if (currentStart < selfCoord.start) {
				results.add(Result.gap(new Gap(currentStart, selfCoord.start - 1)));
				currentStart = selfCoord.start;
			}

			// VEP line 376-407: compute target coordinates
			int targetStart, targetEnd;
			if (pair.ori == 1) {
				targetStart = targetCoord.start + (currentStart - selfCoord.start);
				targetEnd = (end > selfCoord.end)
					? targetCoord.end
					: targetCoord.start + (end - selfCoord.start);
			} else {
				targetEnd = targetCoord.end - (currentStart - selfCoord.start);
				targetStart = (end > selfCoord.end)
					? targetCoord.start
					: targetCoord.end - (end - selfCoord.start);
			}

			results.add(Result.coord(new Coordinate(targetCoord.id, targetStart, targetEnd, pair.ori * strand)));
			lastUsedPair = pair;
			currentStart = selfCoord.end + 1;
		}

		// VEP line 430-444: gap at the end
		if (lastUsedPair == null) {
			results.add(Result.gap(new Gap(start, end)));
		} else {
			CoordRange lastSelf = isFromTo ? lastUsedPair.from : lastUsedPair.to;
			if (lastSelf.end < end) {
				results.add(Result.gap(new Gap(lastSelf.end + 1, end)));
			}
		}

		// VEP line 446-448: reverse if negative strand
		if (strand == -1) {
			java.util.Collections.reverse(results);
		}

		return results;
	}

	/**
	 * VEP Mapper::map_insert (line 484-559).
	 * Handle insertion mapping (start = end + 1).
	 */
	private List<Result> mapInsert(String id, int start, int end, int strand, String type) {
		// VEP line 488: swap start/end
		int swappedStart = end;
		int swappedEnd = start;

		// Map the 2bp coordinate
		List<Result> coords = mapCoordinates(id, swappedStart, swappedEnd, strand, type);

		if (coords.size() == 1) {
			// VEP line 492-501: single result — swap start/end back
			Result r = coords.get(0);
			if (r.isCoordinate()) {
				int tmp = r.coordinate.start;
				r.coordinate.start = r.coordinate.end;
				r.coordinate.end = tmp;
			}
		} else if (coords.size() == 2) {
			// VEP line 503-557: two results — insert at boundary
			Result c1, c2;
			if (strand == -1) {
				c2 = coords.get(0);
				c1 = coords.get(1);
			} else {
				c1 = coords.get(0);
				c2 = coords.get(1);
			}

			List<Result> newCoords = new ArrayList<>();

			if (c1.isCoordinate()) {
				Coordinate m1 = c1.coordinate;
				// VEP line 523-527: insert is after first coord
				if (m1.strand * strand == -1) {
					m1.end--;
				} else {
					m1.start++;
				}
				newCoords.add(Result.coord(m1));
			}

			if (c2.isCoordinate()) {
				Coordinate m2 = c2.coordinate;
				// VEP line 537-541: insert is before second coord
				if (m2.strand * strand == -1) {
					m2.start++;
				} else {
					m2.end--;
				}
				if (strand == -1) {
					newCoords.add(0, Result.coord(m2));
				} else {
					newCoords.add(Result.coord(m2));
				}
			}

			coords = newCoords;
		}

		return coords;
	}

	private List<Pair> getRelevantPairs(String id, boolean isFromTo) {
		List<Pair> relevant = new ArrayList<>();
		for (Pair pair : pairs) {
			CoordRange cr = isFromTo ? pair.from : pair.to;
			if (cr.id.equalsIgnoreCase(id)) {
				relevant.add(pair);
			}
		}
		return relevant;
	}

	private void sort() {
		// Sort pairs by 'from' start for both directions
		pairs.sort(Comparator.comparingInt(p -> p.from.start));
		sorted = true;
	}
}
