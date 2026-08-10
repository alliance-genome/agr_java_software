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
import java.util.function.BiFunction;

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

	public static String buildTextHeader(String filetypeLabel, Format format, String readmeText, Collection<String> taxonCuries, SpeciesLookup species) {
		return buildTextHeader(filetypeLabel, format, readmeText, taxonCuries, species, List.of(), null);
	}

	public static String buildTextHeader(String filetypeLabel, Format format, String readmeText, Collection<String> taxonCuries, SpeciesLookup species, List<String> extraHeaderLines) {
		return buildTextHeader(filetypeLabel, format, readmeText, taxonCuries, species, extraHeaderLines, null);
	}

	public static String buildTextHeader(String filetypeLabel, Format format, String readmeText, Collection<String> taxonCuries, SpeciesLookup species, List<String> extraHeaderLines, BiFunction<String, SpeciesLookup, String> nameResolver) {
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
		if (extraHeaderLines != null) {
			for (String line : extraHeaderLines) {
				if (line != null && !line.isEmpty()) {
					sb.append("# ").append(line).append("\n");
				}
			}
		}
		sb.append("# Taxon IDs: ").append(String.join(", ", taxonCuries)).append("\n");
		sb.append("# Species: ").append(speciesNamesCsv(taxonCuries, species, nameResolver)).append("\n");
		sb.append("# Alliance Database Version: ").append(databaseVersion()).append("\n");
		sb.append("# Date file generated (UTC): ").append(genTime).append("\n");
		sb.append("#\n");
		sb.append("##########################################################################\n");
		return sb.toString();
	}

	public static Map<String, Object> buildJsonMetadata(String filetypeLabel, Format format, String readmeText, Collection<String> taxonCuries, SpeciesLookup species) {
		return buildJsonMetadata(filetypeLabel, format, readmeText, taxonCuries, species, null, null);
	}

	public static Map<String, Object> buildJsonMetadata(String filetypeLabel, Format format, String readmeText, Collection<String> taxonCuries, SpeciesLookup species, String stringencyFilter) {
		return buildJsonMetadata(filetypeLabel, format, readmeText, taxonCuries, species, stringencyFilter, null);
	}

	public static Map<String, Object> buildJsonMetadata(String filetypeLabel, Format format, String readmeText, Collection<String> taxonCuries, SpeciesLookup species, String stringencyFilter, BiFunction<String, SpeciesLookup, String> nameResolver) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("filetype", filetypeLabel);
		metadata.put("databaseVersion", databaseVersion());
		metadata.put("sourceURL", SOURCE_URL);
		metadata.put("genTime", ZonedDateTime.now(ZoneOffset.UTC).format(GEN_TIME_FMT));
		metadata.put("dataFormat", format.getDataFormatLabel());
		metadata.put("stringencyFilter", stringencyFilter);
		metadata.put("readme", readmeText == null ? "" : readmeText);
		List<Map<String, String>> speciesList = new ArrayList<>();
		for (String curie : taxonCuries) {
			Map<String, String> entry = new LinkedHashMap<>();
			entry.put("taxonId", curie);
			entry.put("speciesName", species == null ? "" : nullToEmpty(resolveName(curie, species, nameResolver)));
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

	private static String speciesNamesCsv(Collection<String> taxonCuries, SpeciesLookup lookup, BiFunction<String, SpeciesLookup, String> nameResolver) {
		if (lookup == null) {
			return "";
		}
		List<String> names = new ArrayList<>();
		for (String c : taxonCuries) {
			String n = resolveName(c, lookup, nameResolver);
			if (n != null) {
				names.add(n);
			}
		}
		return String.join(", ", names);
	}

	private static String resolveName(String curie, SpeciesLookup lookup, BiFunction<String, SpeciesLookup, String> nameResolver) {
		if (nameResolver != null) {
			String resolved = nameResolver.apply(curie, lookup);
			if (resolved != null && !resolved.isBlank()) {
				return resolved;
			}
		}
		return lookup.nameFor(curie);
	}

	private static String databaseVersion() {
		String v = ConfigHelper.getAllianceRelease();
		return v == null ? "" : v;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
