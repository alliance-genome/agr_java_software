package org.alliancegenome.filegenerator.tools;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import htsjdk.tribble.TribbleException;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;

/**
 * Standalone VCF validator. Opens each .vcf.gz file with htsjdk and reports header / record parse errors. Run with:
 *
 *   java -cp target/agr_file_generator-jar-with-dependencies.jar org.alliancegenome.filegenerator.tools.VcfValidator <file_or_dir> [...]
 *
 * Each argument can be a single .vcf or .vcf.gz file, or a directory (in which case every .vcf / .vcf.gz under it is scanned). Exit code is 0 only when every file parses cleanly.
 */
public final class VcfValidator {

	private VcfValidator() {
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("usage: VcfValidator <file_or_dir> [more...]");
			System.exit(2);
		}

		List<Path> files = new ArrayList<>();
		for (String a : args) {
			Path p = Paths.get(a);
			if (!Files.exists(p)) {
				System.err.println("skip (not found): " + p);
				continue;
			}
			if (Files.isDirectory(p)) {
				try (Stream<Path> walk = Files.walk(p)) {
					walk.filter(Files::isRegularFile)
							.filter(f -> {
								String n = f.getFileName().toString();
								return n.endsWith(".vcf") || n.endsWith(".vcf.gz");
							})
							.sorted(Comparator.comparing(Path::toString))
							.forEach(files::add);
				} catch (Exception e) {
					System.err.println("walk failed: " + p + " — " + e.getMessage());
				}
			} else {
				files.add(p);
			}
		}

		if (files.isEmpty()) {
			System.err.println("no .vcf / .vcf.gz files found");
			System.exit(2);
		}

		int failed = 0;
		for (Path f : files) {
			Report r = validate(f);
			System.out.println(r.summary());
			if (!r.errors.isEmpty()) {
				failed++;
				for (String e : r.errors) {
					System.out.println("    " + e);
				}
			}
		}

		System.out.println();
		System.out.println(failed == 0
				? "OK: " + files.size() + " file(s) parsed without errors"
				: "FAIL: " + failed + "/" + files.size() + " file(s) had errors");
		System.exit(failed == 0 ? 0 : 1);
	}

	private static Report validate(Path path) {
		Report r = new Report(path);
		long records = 0;
		try (VCFFileReader reader = new VCFFileReader(path.toFile(), false)) {
			VCFHeader header = reader.getFileHeader();
			r.contigCount = header.getContigLines().size();
			r.infoCount = header.getInfoHeaderLines().size();
			r.formatCount = header.getFormatHeaderLines().size();
			r.altCount = header.getMetaDataLine("ALT") != null ? header.getMetaDataInInputOrder().stream().filter(l -> "ALT".equals(l.getKey())).toList().size() : (int) header.getMetaDataInInputOrder().stream().filter(l -> "ALT".equals(l.getKey())).count();
			for (VariantContext vc : reader) {
				records++;
				List<String> issues = recordIssues(vc);
				if (!issues.isEmpty()) {
					for (String issue : issues) {
						r.errors.add("record " + records + " (" + vc.getContig() + ":" + vc.getStart() + " " + vc.getID() + "): " + issue);
						if (r.errors.size() >= 20) {
							r.errors.add("(further errors truncated; " + records + " records scanned so far)");
							r.records = records;
							return r;
						}
					}
				}
			}
		} catch (TribbleException e) {
			r.errors.add("PARSE ERROR at record ~" + (records + 1) + ": " + e.getMessage());
		} catch (Exception e) {
			r.errors.add("UNEXPECTED " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		r.records = records;
		return r;
	}

	private static List<String> recordIssues(VariantContext vc) {
		List<String> out = new ArrayList<>();
		Allele ref = vc.getReference();
		if (ref.getBases().length == 0) {
			out.add("REF is empty");
		}
		if (!isValidBaseString(ref.getBaseString(), true)) {
			out.add("REF contains non-IUPAC base: " + ref.getBaseString());
		}
		for (Allele alt : vc.getAlternateAlleles()) {
			if (alt.isSymbolic()) {
				continue;
			}
			if (alt.getBases().length == 0) {
				out.add("ALT is empty");
				continue;
			}
			if (!isValidBaseString(alt.getBaseString(), false)) {
				out.add("ALT contains non-IUPAC base: " + alt.getBaseString());
			}
		}
		if (vc.getStart() < 1) {
			out.add("POS < 1: " + vc.getStart());
		}
		return out;
	}

	// REF is A/C/G/T/N only per VCF v4.3 §1.4.1.1; ALT additionally permits the ambiguity codes (R/Y/S/W/K/M/B/D/H/V) we declared in the ##ALT header.
	private static boolean isValidBaseString(String s, boolean isRef) {
		for (int i = 0; i < s.length(); i++) {
			char c = Character.toUpperCase(s.charAt(i));
			if (c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N' || c == '*') {
				continue;
			}
			if (!isRef && (c == 'R' || c == 'Y' || c == 'S' || c == 'W' || c == 'K' || c == 'M' || c == 'B' || c == 'D' || c == 'H' || c == 'V')) {
				continue;
			}
			return false;
		}
		return true;
	}

	private static final class Report {
		final Path path;
		long records;
		int contigCount;
		int infoCount;
		int formatCount;
		int altCount;
		final List<String> errors = new ArrayList<>();

		Report(Path path) {
			this.path = path;
		}

		String summary() {
			long bytes = 0;
			try {
				bytes = Files.size(path);
			} catch (Exception ignore) {
				// size is cosmetic — leave at 0 if we can't read it.
			}
			return String.format("%-65s records=%-7d contigs=%-3d info=%-3d alt=%-3d size=%dKB %s",
					path.getFileName(), records, contigCount, infoCount, altCount, bytes / 1024,
					errors.isEmpty() ? "OK" : "ERRORS=" + errors.size());
		}
	}

	@SuppressWarnings("unused")
	private static File toFile(Path p) {
		return p.toFile();
	}
}
