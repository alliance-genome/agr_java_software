package org.alliancegenome.vep.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of Bio::EnsEMBL::TranscriptMapper (541 lines).
 * Uses Mapper (port of Bio::EnsEMBL::Mapper) for coordinate conversions.
 *
 * Coordinate chain: genomic ↔ cDNA ↔ CDS ↔ peptide
 */
public class TranscriptMapper {

	private final Mapper exonCoordMapper;  // VEP line 118: exon_coord_mapper
	private final int startPhase;          // VEP line 119: start_phase
	private final int cdnaCodingStart;     // VEP line 120: cdna_coding_start
	private final int cdnaCodingEnd;       // VEP line 121: cdna_coding_end

	/**
	 * VEP TranscriptMapper::new (line 96-123) + _load_mapper (line 139-234).
	 */
	public TranscriptMapper(TranscriptModel transcript) {
		// VEP line 108-113: start_phase
		if (!transcript.getCdsSegments().isEmpty()) {
			int phase = transcript.getCdsSegments().get(0).getPhase();
			this.startPhase = phase >= 0 ? phase : -1;
		} else {
			this.startPhase = -1;
		}

		// VEP line 120: cdna_coding_start
		this.cdnaCodingStart = transcript.getCdnaCodingStart();

		// VEP line 121: cdna_coding_end = cdna_coding_start + CDS_length - 1
		int cdsLen = 0;
		for (CdsSegment seg : transcript.getCdsSegments()) {
			cdsLen += seg.getEnd() - seg.getStart() + 1;
		}
		this.cdnaCodingEnd = cdnaCodingStart > 0 ? cdnaCodingStart + cdsLen - 1 : 0;

		// VEP _load_mapper (line 139-234): build cdna↔genomic mapper from exons
		// Simplified: no SeqEdits (VEP line 147-222)
		this.exonCoordMapper = new Mapper("cdna", "genomic");
		int cdnaEnd = 0;
		for (ExonModel exon : transcript.getExons()) {
			int genStart = exon.getStart();
			int genEnd = exon.getEnd();
			int cdnaStart = cdnaEnd + 1;
			cdnaEnd = cdnaStart + (genEnd - genStart);
			int strand = transcript.isPositiveStrand() ? 1 : -1;

			// VEP line 228-229: add_map_coordinates('cdna', cdnaStart, cdnaEnd, strand, 'genome', genStart, genEnd)
			exonCoordMapper.addMapCoordinates("cdna", cdnaStart, cdnaEnd, strand,
				"genome", genStart, genEnd);
		}
	}

	/**
	 * VEP genomic2cdna (line 296-307).
	 * $mapper->map_coordinates("genome", $start, $end, $strand, "genomic")
	 */
	public List<Mapper.Result> genomic2cdna(int start, int end, int strand) {
		return exonCoordMapper.mapCoordinates("genome", start, end, strand, "genomic");
	}

	/**
	 * VEP cdna2genomic (line 257-269).
	 * $mapper->map_coordinates('cdna', $start, $end, 1, "cdna")
	 */
	public List<Mapper.Result> cdna2genomic(int start, int end) {
		return exonCoordMapper.mapCoordinates("cdna", start, end, 1, "cdna");
	}

	/**
	 * VEP genomic2cds (line 410-482).
	 * Chains: genomic → cDNA → CDS (subtract cdna_coding_start).
	 */
	public List<Mapper.Result> genomic2cds(int start, int end, int strand) {
		// VEP line 425-428: no coding region
		if (cdnaCodingStart <= 0) {
			return List.of(Mapper.Result.gap(new Mapper.Gap(start, end)));
		}

		List<Mapper.Result> cdnaCoords = genomic2cdna(start, end, strand);
		List<Mapper.Result> out = new ArrayList<>();

		for (Mapper.Result r : cdnaCoords) {
			if (r.isGap()) {
				out.add(r);
			} else {
				Mapper.Coordinate coord = r.coordinate;
				int cStart = coord.start;
				int cEnd = coord.end;

				// VEP line 441: outside coding region
				if (coord.strand == -1 || cEnd < cdnaCodingStart || cStart > cdnaCodingEnd) {
					out.add(Mapper.Result.gap(new Mapper.Gap(cStart, cEnd)));
				} else {
					// VEP line 447-448: convert to CDS
					int cdsStart = cStart - cdnaCodingStart + 1;
					int cdsEnd = cEnd - cdnaCodingStart + 1;

					// VEP line 450-456: 5' UTR overlap
					if (cStart < cdnaCodingStart) {
						out.add(Mapper.Result.gap(new Mapper.Gap(cStart, cdnaCodingStart - 1)));
						cdsStart = 1;
					}

					// VEP line 459-464: 3' UTR overlap
					Mapper.Result endGap = null;
					if (cEnd > cdnaCodingEnd) {
						endGap = Mapper.Result.gap(new Mapper.Gap(cdnaCodingEnd + 1, cEnd));
						cdsEnd = cdnaCodingEnd - cdnaCodingStart + 1;
					}

					// VEP line 467-470
					out.add(Mapper.Result.coord(new Mapper.Coordinate(coord.id, cdsStart, cdsEnd, coord.strand)));

					if (endGap != null) {
						out.add(endGap);
					}
				}
			}
		}

		return out;
	}

	/**
	 * VEP genomic2pep (line 504-538).
	 * Chains: genomic → CDS → peptide (divide by 3 with phase).
	 */
	public List<Mapper.Result> genomic2pep(int start, int end, int strand) {
		List<Mapper.Result> cdsCoords = genomic2cds(start, end, strand);
		List<Mapper.Result> out = new ArrayList<>();

		// VEP line 518: phase shift
		int shift = (startPhase > 0) ? startPhase : 0;

		for (Mapper.Result r : cdsCoords) {
			if (r.isGap()) {
				out.add(r);
			} else {
				// VEP line 528-529
				int pepStart = (r.coordinate.start + shift + 2) / 3;
				int pepEnd = (r.coordinate.end + shift + 2) / 3;
				out.add(Mapper.Result.coord(
					new Mapper.Coordinate(r.coordinate.id, pepStart, pepEnd, r.coordinate.strand)));
			}
		}

		return out;
	}

	/**
	 * VEP cds2genomic (line 330-352).
	 */
	public List<Mapper.Result> cds2genomic(int start, int end) {
		if (cdnaCodingStart <= 0) return List.of();

		// VEP line 338-339: CDS → cDNA
		int cdnaStart = start + (cdnaCodingStart - 1);
		int cdnaEnd = end + (cdnaCodingStart - 1);

		if (cdnaStart > cdnaCodingEnd) return List.of();
		if (cdnaEnd > cdnaCodingEnd) cdnaEnd = cdnaCodingEnd;

		return cdna2genomic(cdnaStart, cdnaEnd);
	}

	/**
	 * VEP pep2genomic (line 372-388).
	 */
	public List<Mapper.Result> pep2genomic(int start, int end) {
		int shift = (startPhase > 0) ? startPhase : 0;

		// VEP line 384-385: peptide → cDNA
		int cdnaStart = 3 * start - 2 + (cdnaCodingStart - 1) - shift;
		int cdnaEnd = 3 * end + (cdnaCodingStart - 1) - shift;

		return cdna2genomic(cdnaStart, cdnaEnd);
	}

	// Getters
	public int getStartPhase() { return startPhase; }
	public int getCdnaCodingStart() { return cdnaCodingStart; }
	public int getCdnaCodingEnd() { return cdnaCodingEnd; }
}
