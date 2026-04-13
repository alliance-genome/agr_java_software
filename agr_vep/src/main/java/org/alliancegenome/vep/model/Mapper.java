package org.alliancegenome.vep.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alliancegenome.vep.debug.Trace;

/**
 * Port of Bio::EnsEMBL::Mapper (1257 lines).
 * Generic coordinate mapping between two coordinate systems.
 *
 * Stores pairs of coordinates (from/to) and maps positions between them.
 * Handles both normal mappings and insertions (start = end + 1).
 *
 * Uses dual-direction per-ID hash maps matching Perl's _pair_$from and _pair_$to
 * for O(1) lookup by ID in either direction.
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
	static class Pair {
		CoordRange from;
		CoordRange to;
		int ori; // relative orientation: 1 or -1

		Pair(CoordRange from, CoordRange to, int ori) {
			this.from = from;
			this.to = to;
			this.ori = ori;
		}
	}

	static class CoordRange {
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
	private final Map<String, List<Pair>> fromPairs = new HashMap<>();
	private final Map<String, List<Pair>> toPairs = new HashMap<>();
	private boolean sorted = false;

	public Mapper(String from, String to) {
		this.fromName = from;
		this.toName = to;
	}

	/**
	 * VEP Mapper::add_map_coordinates (line 665-720).
	 * Add a coordinate pair mapping.
	 * The "from" side is contigId/contigStart/contigEnd,
	 * the "to" side is chrName/chrStart/chrEnd.
	 */
	public void addMapCoordinates(String contigId, int contigStart, int contigEnd, int strand,
			String chrName, int chrStart, int chrEnd) {
		CoordRange from = new CoordRange(contigId, contigStart, contigEnd);
		CoordRange to = new CoordRange(chrName, chrStart, chrEnd);
		int ori = strand; // +1 or -1
		Pair pair = new Pair(from, to, ori);
		// Perl line 717-718: store in both direction maps
		fromPairs.computeIfAbsent(contigId.toUpperCase(), k -> new ArrayList<>()).add(pair);
		toPairs.computeIfAbsent(chrName.toUpperCase(), k -> new ArrayList<>()).add(pair);
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
		Trace.log("Mapper.map_coordinates",
			"id=%s start=%d end=%d strand=%d type=%s from=%s to=%s",
			id, start, end, strand, type, fromName, toName);

		// VEP line 267-269: special case for insertions (start = end + 1)
		if (start == end + 1) {
			List<Result> r = mapInsert(id, start, end, strand, type);
			traceResults(r);
			return r;
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
			Collections.reverse(results);
		}

		traceResults(results);
		return results;
	}

	private static void traceResults(List<Result> results) {
		if (!Trace.enabled()) return;
		for (Result r : results) {
			if (r.isCoordinate()) {
				Trace.log("  -> Coord",
					"id=%s start=%d end=%d strand=%d",
					r.coordinate.id, r.coordinate.start, r.coordinate.end, r.coordinate.strand);
			} else {
				Trace.log("  -> Gap",
					"start=%d end=%d", r.gap.start, r.gap.end);
			}
		}
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

	/**
	 * Perl line 1095-1115: look up relevant pairs by ID from the correct direction map.
	 */
	private List<Pair> getRelevantPairs(String id, boolean isFromTo) {
		Map<String, List<Pair>> hash = isFromTo ? fromPairs : toPairs;
		List<Pair> pairs = hash.get(id.toUpperCase());
		return pairs != null ? pairs : new ArrayList<>();
	}

	/**
	 * Perl line 1095-1115: sort each map by its own coordinate system.
	 * From-pairs sorted by from.start, to-pairs sorted by to.start.
	 */
	private void sort() {
		for (List<Pair> list : fromPairs.values()) {
			list.sort((a, b) -> Integer.compare(a.from.start, b.from.start));
		}
		for (List<Pair> list : toPairs.values()) {
			list.sort((a, b) -> Integer.compare(a.to.start, b.to.start));
		}
		mergePairs();
		sorted = true;
	}

	/**
	 * Perl _merge_pairs (line 1118-1200).
	 * Merge adjacent pairs with same from.id, same ori, and contiguous to coordinates.
	 * Also handles duplicates (same to.start, same from.id, same from.start).
	 */
	private void mergePairs() {
		for (Map.Entry<String, List<Pair>> entry : toPairs.entrySet()) {
			List<Pair> list = entry.getValue();
			if (list.size() < 2) continue;

			int i = 0;
			while (i < list.size() - 1) {
				Pair current = list.get(i);
				Pair next = list.get(i + 1);

				// Skip if different from.id or different orientation
				if (!current.from.id.equalsIgnoreCase(next.from.id) || current.ori != next.ori) {
					i++;
					continue;
				}

				// Check for duplicate: same to.start, same from.start
				if (current.to.start == next.to.start
						&& current.from.start == next.from.start) {
					// Remove duplicate from both maps
					list.remove(i + 1);
					removeFromMap(fromPairs, next);
					continue;
				}

				// Check if contiguous in 'to' coordinates
				if (next.to.start - 1 != current.to.end) {
					i++;
					continue;
				}

				// Check if contiguous in 'from' coordinates based on orientation
				boolean contiguous;
				if (current.ori == 1) {
					// Forward strand: next.from.start - 1 == current.from.end
					contiguous = next.from.start - 1 == current.from.end;
				} else {
					// Reverse strand: next.from.end + 1 == current.from.start
					contiguous = next.from.end + 1 == current.from.start;
				}

				if (contiguous) {
					// Merge: extend current pair's ranges
					current.to.end = next.to.end;
					if (current.ori == 1) {
						current.from.end = next.from.end;
					} else {
						current.from.start = next.from.start;
					}
					// Remove next from both maps
					list.remove(i + 1);
					removeFromMap(fromPairs, next);
				} else {
					i++;
				}
			}
		}
	}

	/**
	 * Remove a specific Pair instance from a direction map.
	 */
	private void removeFromMap(Map<String, List<Pair>> map, Pair pair) {
		String key = pair.from.id.toUpperCase();
		List<Pair> list = map.get(key);
		if (list != null) {
			Iterator<Pair> it = list.iterator();
			while (it.hasNext()) {
				if (it.next() == pair) {
					it.remove();
					break;
				}
			}
		}
	}
}
