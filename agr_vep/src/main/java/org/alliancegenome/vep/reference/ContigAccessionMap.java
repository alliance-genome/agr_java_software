package org.alliancegenome.vep.reference;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ContigAccessionMap {

	private final Map<String, String> chrToAccession = new HashMap<>();
	private final Map<String, String> accessionToChr = new HashMap<>();

	public static ContigAccessionMap fromFasta(String fastaPath) throws Exception {
		return fromFasta(fastaPath, null);
	}

	public static ContigAccessionMap fromFasta(String fastaPath, String synonymsFilePath) throws Exception {
		ContigAccessionMap map = new ContigAccessionMap();

		// First load from FASTA headers (e.g., ">chrI NC_001133")
		try (BufferedReader reader = new BufferedReader(new FileReader(fastaPath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith(">")) {
					String[] parts = line.substring(1).split("\\s+");
					if (parts.length >= 2) {
						String chr = parts[0];
						String accession = parts[1];
						if (accession.startsWith("NC_") || accession.startsWith("NT_")
								|| accession.startsWith("NW_")) {
							map.put(chr, accession);
						}
					}
				}
			}
		}
		log.info("Contig accession map: {} mappings from FASTA headers", map.size());

		// Then load chr_synonyms.txt if available (VEP cache file)
		// Format: tab-delimited bidirectional pairs, e.g., "I\tNC_003279.8"
		if (synonymsFilePath != null) {
			try (BufferedReader reader = new BufferedReader(new FileReader(synonymsFilePath))) {
				String line;
				int synonymCount = 0;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#")) continue;
					String[] parts = line.split("\\s+");
					if (parts.length >= 2) {
						String a = parts[0];
						String b = parts[1];
						// Find the NC_ accession and the chromosome name
						String nc = null, chr = null;
						if (a.startsWith("NC_") || a.startsWith("NT_") || a.startsWith("NW_")) {
							nc = a; chr = b;
						} else if (b.startsWith("NC_") || b.startsWith("NT_") || b.startsWith("NW_")) {
							nc = b; chr = a;
						}
						if (nc != null && chr != null) {
							// Prefer the VERSIONED accession (e.g. NT_033779.5 over NT_033779)
							// VEP uses RefSeq_genomic synonyms which are typically versioned.
							String existing = map.chrToAccession.get(chr);
							boolean newHasVersion = nc.contains(".");
							boolean existingHasVersion = existing != null && existing.contains(".");
							if (existing == null || (newHasVersion && !existingHasVersion)) {
								map.put(chr, nc);
								synonymCount++;
							}
						}
					}
				}
				log.info("Contig accession map: {} additional mappings from chr_synonyms.txt", synonymCount);
			} catch (Exception e) {
				log.warn("Could not read chr_synonyms file {}: {}", synonymsFilePath, e.getMessage());
			}
		}

		log.info("Contig accession map total: {} mappings", map.size());
		return map;
	}

	public void put(String chr, String accession) {
		chrToAccession.put(chr, accession);
		accessionToChr.put(accession, chr);
	}

	public String getAccession(String chr) {
		String acc = chrToAccession.get(chr);
		if (acc == null) {
			// Try case-insensitive lookup
			for (Map.Entry<String, String> entry : chrToAccession.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(chr)) {
					return entry.getValue();
				}
			}
		}
		// Fallback: use chromosome name directly when no NC_ accession available
		// VEP does this via chr_synonyms.txt or the chromosome name from the cache
		if (acc == null) {
			return chr;
		}
		return acc;
	}

	public String getChromosome(String accession) {
		return accessionToChr.get(accession);
	}

	public int size() {
		return chrToAccession.size();
	}
}
