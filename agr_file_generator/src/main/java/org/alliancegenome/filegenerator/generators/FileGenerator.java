package org.alliancegenome.filegenerator.generators;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.filegenerator.config.FileGeneratorConfig;
import org.alliancegenome.filegenerator.config.Format;
import org.alliancegenome.filegenerator.config.OutputSpec;
import org.alliancegenome.filegenerator.config.SplitMode;
import org.alliancegenome.filegenerator.es.EsParallelFetcher;
import org.alliancegenome.filegenerator.header.HeaderBuilder;
import org.alliancegenome.filegenerator.species.SpeciesLookup;
import org.alliancegenome.filegenerator.writers.JsonMappedWriter;
import org.alliancegenome.filegenerator.writers.JsonPath;
import org.alliancegenome.filegenerator.writers.JsonRawWriter;
import org.alliancegenome.filegenerator.writers.RowWriter;
import org.alliancegenome.filegenerator.writers.TsvWriter;
import org.alliancegenome.filegenerator.writers.TxtWriter;
import org.alliancegenome.filegenerator.writers.VcfWriter;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class FileGenerator extends Thread {

	protected final FileGeneratorConfig config;
	protected ProcessDisplayHelper display = new ProcessDisplayHelper();
	protected SpeciesLookup species;

	@Getter
	private Duration duration = Duration.ZERO;

	public FileGenerator(FileGeneratorConfig config) {
		this.config = config;
	}

	@Override
	public void run() {
		super.run();
		try {
			Instant start = Instant.now();
			ensureOutputFolder();
			// scrollAndWrite() starts/finishes the display with a known total; stub generators
			// just log and return without using the helper.
			generate();
			Instant end = Instant.now();
			duration = Duration.between(start, end);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			ExceptionCatcher.report(e);
			System.exit(-1);
		}
	}

	protected abstract void generate() throws Exception;

	/**
	 * Default ES parallel-paginated fetch + dispatch pipeline. Subclasses with fully-implemented
	 * logic call this from generate(). Stubs do not.
	 */
	protected void scrollAndWrite() throws Exception {
		species = new SpeciesLookup();
		String readme = HeaderBuilder.loadReadme(config.getReadmeResourcePath());
		Path outDir = getGeneratedFilesFolder();

		// Taxon curies for the allowed MODs only — used to populate the COMBINED file's species block.
		Set<String> allowedMods = new LinkedHashSet<>(config.getMods());
		List<String> combinedTaxonCuries = new ArrayList<>();
		for (Map.Entry<String, String> e : species.taxonToMod().entrySet()) {
			if (allowedMods.contains(e.getValue())) {
				combinedTaxonCuries.add(e.getKey());
			}
		}

		Map<OutputSpec, ConcurrentHashMap<String, RowWriter>> writersBySpec = new LinkedHashMap<>();
		for (OutputSpec spec : config.getOutputs()) {
			ConcurrentHashMap<String, RowWriter> bySubtype = new ConcurrentHashMap<>();
			writersBySpec.put(spec, bySubtype);
			if (spec.split() == SplitMode.COMBINED) {
				RowWriter w = openWriter(spec, "COMBINED", combinedTaxonCuries, outDir, readme);
				bySubtype.put("COMBINED", w);
			}
		}

		try {
			EsParallelFetcher fetcher = new EsParallelFetcher(esIndex(), config.getEsCategories());
			// Run a _count up front so the progress display can render against a known total.
			long total = fetcher.count();
			log.info("{}: ES _count for {} = {}", getClass().getSimpleName(), config.getEsCategories(), total);
			if (total > 0) {
				display.startProcess(getClass().getSimpleName(), total);
			} else {
				display.startProcess(getClass().getSimpleName());
			}
			fetcher.forEach(config.getThreadCount(), config.getBufferSize(), computeSourceIncludes(),
					hit -> dispatch(hit, writersBySpec, outDir, readme));
			display.finishProcess();
		} finally {
			for (Map<String, RowWriter> writers : writersBySpec.values()) {
				for (RowWriter w : writers.values()) {
					try {
						w.close();
						log.info("Wrote {} rows -> {}", w.getRowCount(), w.getPath());
					} catch (IOException e) {
						log.warn("Failed to close writer {}: {}", w.getPath(), e.getMessage());
					}
				}
			}
		}
	}

	private void dispatch(JsonNode hit, Map<OutputSpec, ConcurrentHashMap<String, RowWriter>> writersBySpec, Path outDir, String readme) {
		try {
			if (!shouldEmit(hit)) {
				return;
			}
			JsonNode row = customizeRow(hit);

			// Doc-level taxa: used to route doc-format outputs (JSON_RAW / VCF / GFF) which always emit the consolidated row verbatim. Order is preserved so the first taxon mapping to an allowed MOD wins the per-MOD file header.
			LinkedHashSet<String> docMods = new LinkedHashSet<>();
			String docPrimaryTaxon = collectAllowedMods(resolveTaxonCurie(row), additionalTaxonCuries(row), docMods);

			List<JsonNode> expanded = null;
			for (OutputSpec spec : config.getOutputs()) {
				ConcurrentHashMap<String, RowWriter> writers = writersBySpec.get(spec);

				if (!spec.format().isRowFormat()) {
					// Doc-format path: route the consolidated row by doc-level taxa.
					if (docMods.isEmpty()) {
						continue;
					}
					if (spec.split() == SplitMode.COMBINED) {
						writers.get("COMBINED").writeRow(row);
					} else {
						String taxonForHeader = docPrimaryTaxon;
						for (String mod : docMods) {
							RowWriter w = writers.computeIfAbsent(mod, m -> openWriterUnchecked(spec, m, List.of(taxonForHeader), outDir, readme));
							w.writeRow(row);
						}
					}
					continue;
				}

				// Row-format path: deconsolidate via customizeRows, then route EACH row by its own taxon. Generators that don't deconsolidate get a singleton list whose row carries the same taxon as the doc, so the result is identical to the previous doc-level routing.
				if (expanded == null) {
					expanded = customizeRows(row);
				}
				if (expanded.isEmpty()) {
					continue;
				}
				if (spec.split() == SplitMode.COMBINED) {
					RowWriter w = writers.get("COMBINED");
					for (JsonNode r : expanded) {
						w.writeRow(r);
					}
					continue;
				}
				for (JsonNode r : expanded) {
					LinkedHashSet<String> rowMods = new LinkedHashSet<>();
					String rowPrimaryTaxon = collectAllowedMods(resolveRowTaxonCurie(r), additionalRowTaxonCuries(r), rowMods);
					if (rowMods.isEmpty()) {
						continue;
					}
					String taxonForHeader = rowPrimaryTaxon;
					for (String mod : rowMods) {
						RowWriter w = writers.computeIfAbsent(mod, m -> openWriterUnchecked(spec, m, List.of(taxonForHeader), outDir, readme));
						w.writeRow(r);
					}
				}
			}
			display.progressProcess();
		} catch (Exception e) {
			// Crash hard on any per-row failure — a single bad row invalidates the run, and the partial gzip on disk must be left broken so it cannot be uploaded.
			log.error("{}: dispatch failed for row {} — exiting hard. Cause: {}", getClass().getSimpleName(), hit, e.getMessage(), e);
			System.exit(-1);
		}
	}

	/**
	 * Walks the primary taxon + any extras, intersects with the configured MOD whitelist, and fills {@code out} with the allowed MODs in insertion order. Returns the first taxon curie whose MOD survived the whitelist (used for the per-MOD file header); null if none did.
	 */
	private String collectAllowedMods(String primaryTaxon, List<String> extras, LinkedHashSet<String> out) {
		LinkedHashSet<String> taxa = new LinkedHashSet<>();
		if (primaryTaxon != null) {
			taxa.add(primaryTaxon);
		}
		if (extras != null) {
			for (String extra : extras) {
				if (extra != null) {
					taxa.add(extra);
				}
			}
		}
		String anyAllowed = null;
		for (String curie : taxa) {
			String mod = species.modFor(curie);
			if (mod != null && config.getMods().contains(mod)) {
				out.add(mod);
				if (anyAllowed == null) {
					anyAllowed = curie;
				}
			}
		}
		return anyAllowed;
	}

	/**
	 * Override to declare extra taxon curies a hit should also be dispatched to. Default empty.
	 * Used by Interactions where one ES doc represents an A↔B pair across two species.
	 */
	protected List<String> additionalTaxonCuries(JsonNode hit) {
		return List.of();
	}

	/**
	 * Override to filter out hits before any work happens. Default keeps every hit. Orthology
	 * uses this to skip moderate / all-stringency rows and only emit `stringent` orthologs.
	 */
	protected boolean shouldEmit(JsonNode hit) {
		return true;
	}

	/**
	 * The ES index (or wildcard pattern) this generator scrolls. Default is whatever
	 * {@link ConfigHelper#getEsIndex()} returns (the {@code site_index} alias). VCF overrides
	 * to {@code site_index_stage_*} so the wildcard excludes the HTP {@code variant_index_*}
	 * that's also aliased as {@code site_index}.
	 */
	protected String esIndex() {
		return ConfigHelper.getEsIndex();
	}

	protected String resolveTaxonCurie(JsonNode hit) {
		String fromPath = JsonPath.resolveString(hit, taxonPath());
		if (fromPath != null && !fromPath.isEmpty()) {
			return fromPath;
		}
		String name = JsonPath.resolveString(hit, "species");
		if (name != null && !name.isEmpty() && species != null) {
			return species.taxonForName(name);
		}
		return null;
	}

	protected String taxonPath() {
		return "gene.taxon.curie";
	}

	/**
	 * Row-level taxon JSON path used by row-format outputs (TSV / TXT / JSON_MAPPED) AFTER customizeRows() has expanded a consolidated ES doc. Default delegates to {@link #taxonPath()} so generators that don't deconsolidate behave identically to before. Deconsolidating generators (Disease / Phenotype) override this to point at a synthetic per-row field like {@code _taxon} that customizeRows() populates from the individual primaryAnnotations[i] entry.
	 */
	protected String rowTaxonPath() {
		return taxonPath();
	}

	/**
	 * Row-level companion to {@link #additionalTaxonCuries(JsonNode)}. Default delegates so multi-taxon dispatch on the doc level (e.g. Orthology's HGNC↔MGI pair) is preserved automatically for generators that don't deconsolidate.
	 */
	protected List<String> additionalRowTaxonCuries(JsonNode row) {
		return additionalTaxonCuries(row);
	}

	protected String resolveRowTaxonCurie(JsonNode row) {
		String fromPath = JsonPath.resolveString(row, rowTaxonPath());
		if (fromPath != null && !fromPath.isEmpty()) {
			return fromPath;
		}
		String name = JsonPath.resolveString(row, "species");
		if (name != null && !name.isEmpty() && species != null) {
			return species.taxonForName(name);
		}
		return null;
	}

	protected JsonNode customizeRow(JsonNode hit) {
		return hit;
	}

	/**
	 * Override for generators whose ES docs are consolidated and need to be expanded into multiple flattened rows for row-formats (TSV / TXT / JSON_MAPPED). Default returns a singleton list with the customized hit unchanged, preserving today's one-row-per-hit behavior. Doc formats (JSON_RAW / VCF / GFF) never call this — they always write the consolidated row verbatim.
	 */
	protected List<JsonNode> customizeRows(JsonNode customizedHit) {
		return List.of(customizedHit);
	}

	/**
	 * Override to declare extra ES _source paths a generator needs that aren't in its field map
	 * (e.g. fallback fields used inside customizeRow). Returns an empty list by default.
	 */
	protected List<String> additionalSourceIncludes() {
		return List.of();
	}

	/**
	 * Per-MOD header placeholder substitutions for format templates that support them (currently VCF only). Default empty so existing generators are unaffected. VCF generator overrides this to emit `{contigLines}` per MOD via a one-shot ES aggregation.
	 */
	protected Map<String, String> headerSubstitutions(String mod) {
		return Map.of();
	}

	/** Stringency filter value for the JSON metadata header. Null means "not applicable" for this generator. */
	protected String stringencyFilter() {
		return null;
	}

	/** Override the `readme` field used in JSON metadata headers. Default returns null, which means use the TSV readme text. Generators that publish a LinkML schema URL (e.g. Gene) override this. */
	protected String jsonReadmeOverride() {
		return null;
	}

	/** Extra `# ...` lines to inject into the text header block (TSV / TXT) between Help Desk and Taxon IDs. Default empty. Orthology overrides to emit `Orthology Filter: Stringent` to match FMS. */
	protected List<String> extraHeaderLines() {
		return List.of();
	}

	/** Resolves the species display name shown in `# Species:` and JSON metadata. Defaults to {@code SpeciesLookup.fullNameFor} so yeast renders as "Saccharomyces cerevisiae" instead of strain-suffixed "Saccharomyces cerevisiae S288C". HeaderBuilder falls back to the short name when fullName is missing. */
	protected BiFunction<String, SpeciesLookup, String> speciesNameResolver() {
		return (curie, lookup) -> lookup == null ? null : lookup.fullNameFor(curie);
	}

	/**
	 * Builds the ES _source include list from the field map, taxon path, and any extras. Returns
	 * null when any output requires the full source (JSON_RAW, VCF, GFF) — null tells the fetcher
	 * to skip _source filtering.
	 */
	private List<String> computeSourceIncludes() {
		for (OutputSpec spec : config.getOutputs()) {
			Format f = spec.format();
			if (f == Format.JSON_RAW || f == Format.VCF || f == Format.GFF) {
				return null;
			}
		}
		Set<String> includes = new LinkedHashSet<>();
		includes.addAll(config.getFieldMap().values());
		String tp = taxonPath();
		if (tp != null && !tp.isEmpty()) {
			includes.add(tp);
		}
		includes.add("species");
		includes.addAll(additionalSourceIncludes());
		includes.removeIf(s -> s == null || s.isEmpty());
		return new ArrayList<>(includes);
	}

	private RowWriter openWriterUnchecked(OutputSpec spec, String subtype, Collection<String> taxonCuries, Path outDir, String readme) {
		try {
			return openWriter(spec, subtype, taxonCuries, outDir, readme);
		} catch (IOException e) {
			throw new RuntimeException("Failed to open writer for " + spec.format() + " " + subtype, e);
		}
	}

	private RowWriter openWriter(OutputSpec spec, String subtype, Collection<String> taxonCuries, Path outDir, String readme) throws IOException {
		String fileName = config.getType() + "_" + spec.format().getFiletypeToken() + "_" + subtype + "." + spec.format().getExtension() + ".gz";
		Path path = outDir.resolve(fileName);
		switch (spec.format()) {
			case TSV: {
				String header = HeaderBuilder.buildTextHeader(config.getFiletypeLabel(), spec.format(), readme, taxonCuries, species, extraHeaderLines(), speciesNameResolver());
				return new TsvWriter(path, header, config.getFieldMap());
			}
			case TXT: {
				String header = HeaderBuilder.buildTextHeader(config.getFiletypeLabel(), spec.format(), readme, taxonCuries, species, extraHeaderLines(), speciesNameResolver());
				return new TxtWriter(path, header, config.getFieldMap());
			}
			case JSON_RAW: {
				String jsonReadme = jsonReadmeOverride() != null ? jsonReadmeOverride() : readme;
				Map<String, Object> meta = HeaderBuilder.buildJsonMetadata(config.getFiletypeLabel(), spec.format(), jsonReadme, taxonCuries, species, stringencyFilter(), speciesNameResolver());
				return new JsonRawWriter(path, meta);
			}
			case JSON_MAPPED: {
				String jsonReadme = jsonReadmeOverride() != null ? jsonReadmeOverride() : readme;
				Map<String, Object> meta = HeaderBuilder.buildJsonMetadata(config.getFiletypeLabel(), spec.format(), jsonReadme, taxonCuries, species, stringencyFilter(), speciesNameResolver());
				return new JsonMappedWriter(path, meta, config.getFieldMap());
			}
			case VCF: {
				return new VcfWriter(path, config.getFieldMap(), headerSubstitutions(subtype));
			}
			default:
				throw new UnsupportedOperationException("Format not yet supported: " + spec.format());
		}
	}

	protected Path getGeneratedFilesFolder() {
		return Paths.get(ConfigHelper.getGeneratedFilesFolder());
	}

	private void ensureOutputFolder() throws IOException {
		Files.createDirectories(getGeneratedFilesFolder());
	}
}
