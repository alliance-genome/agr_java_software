package org.alliancegenome.filegenerator.header;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.filegenerator.config.Format;
import org.alliancegenome.filegenerator.species.SpeciesLookup;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeaderBuilder {

	private static final String SOURCE_URL = "https://www.alliancegenome.org/downloads";
	private static final String HELP_DESK = "help@alliancegenome.org";
	private static final DateTimeFormatter GEN_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private HeaderBuilder() {
	}

	public static String buildPsiMiTabHeader(String filetypeLabel, String readmeUrl, Collection<String> taxonCuries, SpeciesLookup species) {
		String genTime = ZonedDateTime.now(ZoneOffset.UTC).format(GEN_TIME_FMT);
		StringBuilder sb = new StringBuilder();
		sb.append("##########################################################################\n");
		sb.append("#\n");
		sb.append("# Data type: ").append(filetypeLabel).append("\n");
		sb.append("# Data format: PSI-MI TAB 2.7 Format\n");
		sb.append("# README: ").append(readmeUrl == null ? "" : readmeUrl).append("\n");
		sb.append("# Source: Alliance of Genome Resources (Alliance)\n");
		sb.append("# Source URL: ").append(SOURCE_URL).append("\n");
		sb.append("# Help Desk: ").append(HELP_DESK).append("\n");
		sb.append("# TaxonIDs: ").append(joinNcbiTxids(taxonCuries)).append("\n");
		sb.append("# Species: ").append(speciesNamesCsv(taxonCuries, species)).append("\n");
		sb.append("# Alliance Database Version: ").append(databaseVersion()).append("\n");
		sb.append("# File generated (UTC): ").append(genTime).append("\n");
		sb.append("#\n");
		sb.append("##########################################################################");
		return sb.toString();
	}

	private static String joinNcbiTxids(Collection<String> taxonCuries) {
		List<String> out = new ArrayList<>();
		for (String c : taxonCuries) {
			if (c == null) {
				continue;
			}
			String txid = c.replace("NCBITaxon:", "");
			out.add("NCBI:txid" + txid);
		}
		return String.join(", ", out);
	}

	public static String buildTextHeader(String filetypeLabel, Format format, String readmeText, Collection<String> taxonCuries, SpeciesLookup species) {
		String genTime = ZonedDateTime.now(ZoneOffset.UTC).format(GEN_TIME_FMT);
		StringBuilder sb = new StringBuilder();
		sb.append("##########################################################################\n");
		sb.append("#\n");
		sb.append("# Data type: ").append(filetypeLabel).append("\n");
		sb.append("# Data format: ").append(format.getDataFormatLabel()).append("\n");
		sb.append("# README: ").append(readmeText == null ? "" : readmeText).append("\n");
		sb.append("# Source: Alliance of Genome Resources (Alliance)\n");
		sb.append("# Source URL: ").append(SOURCE_URL).append("\n");
		sb.append("# Help Desk: ").append(HELP_DESK).append("\n");
		sb.append("# Taxon IDs: ").append(String.join(", ", taxonCuries)).append("\n");
		sb.append("# Species: ").append(speciesNamesCsv(taxonCuries, species)).append("\n");
		sb.append("# Alliance Database Version: ").append(databaseVersion()).append("\n");
		sb.append("# Date file generated (UTC): ").append(genTime).append("\n");
		sb.append("#\n");
		sb.append("##########################################################################\n");
		return sb.toString();
	}

	public static Map<String, Object> buildJsonMetadata(String filetypeLabel, Format format, String readmeText, Collection<String> taxonCuries, SpeciesLookup species) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("filetype", filetypeLabel);
		metadata.put("databaseVersion", databaseVersion());
		metadata.put("sourceURL", SOURCE_URL);
		metadata.put("genTime", ZonedDateTime.now(ZoneOffset.UTC).format(GEN_TIME_FMT));
		metadata.put("dataFormat", format.getDataFormatLabel());
		metadata.put("readme", readmeText == null ? "" : readmeText);
		List<Map<String, String>> speciesList = new ArrayList<>();
		for (String curie : taxonCuries) {
			Map<String, String> entry = new LinkedHashMap<>();
			entry.put("taxonId", curie);
			entry.put("speciesName", species == null ? "" : nullToEmpty(species.nameFor(curie)));
			speciesList.add(entry);
		}
		metadata.put("species", speciesList);
		return metadata;
	}

	public static String loadReadme(String resourcePath) {
		if (resourcePath == null || resourcePath.isEmpty()) {
			return "";
		}
		try (InputStream in = HeaderBuilder.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (in == null) {
				log.warn("README resource not found: {}", resourcePath);
				return "";
			}
			StringBuilder out = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				String line;
				boolean first = true;
				while ((line = reader.readLine()) != null) {
					if (!first) {
						out.append(" ");
					}
					out.append(line);
					first = false;
				}
			}
			return out.toString();
		} catch (IOException e) {
			log.warn("Failed to read README {}: {}", resourcePath, e.getMessage());
			return "";
		}
	}

	private static String speciesNamesCsv(Collection<String> taxonCuries, SpeciesLookup lookup) {
		if (lookup == null) {
			return "";
		}
		List<String> names = new ArrayList<>();
		for (String c : taxonCuries) {
			String n = lookup.nameFor(c);
			if (n != null) {
				names.add(n);
			}
		}
		return String.join(", ", names);
	}

	private static String databaseVersion() {
		String v = ConfigHelper.getAllianceRelease();
		return v == null ? "" : v;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
