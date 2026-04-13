package org.alliancegenome.vep.vcf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.alliancegenome.vep.annotation.OutputFactory;
import org.alliancegenome.vep.csq.CsqEntry;
import org.alliancegenome.vep.csq.CsqHeaderWriter;
import org.alliancegenome.vep.gff.Gff3GeneModelBuilder;
import org.alliancegenome.vep.model.GeneModel;
import org.alliancegenome.vep.plugin.PredictionLookup;
import org.alliancegenome.vep.reference.ContigAccessionMap;
import org.alliancegenome.vep.reference.ReferenceGenome;

import java.nio.file.Path;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import lombok.extern.log4j.Log4j2;
import net.nilosplace.process_display.ProcessDisplayHelper;

@Log4j2
public class VcfAnnotationPipeline {

	private final String vcfPath;
	private final String gffPath;
	private final String fastaPath;
	private final String bamPath;
	private final String outputPath;
	private final String mod;
	private final String mMapPath;
	private final String synonymsPath;
	private final String tmapPath;

	public VcfAnnotationPipeline(String vcfPath, String gffPath, String fastaPath,
			String bamPath, String outputPath, String mod, String mMapPath) {
		this(vcfPath, gffPath, fastaPath, bamPath, outputPath, mod, mMapPath, null, null);
	}

	public VcfAnnotationPipeline(String vcfPath, String gffPath, String fastaPath,
			String bamPath, String outputPath, String mod, String mMapPath, String synonymsPath) {
		this(vcfPath, gffPath, fastaPath, bamPath, outputPath, mod, mMapPath, synonymsPath, null);
	}

	public VcfAnnotationPipeline(String vcfPath, String gffPath, String fastaPath,
			String bamPath, String outputPath, String mod, String mMapPath, String synonymsPath, String tmapPath) {
		this.vcfPath = vcfPath;
		this.gffPath = gffPath;
		this.fastaPath = fastaPath;
		this.bamPath = bamPath;
		this.outputPath = outputPath;
		this.mod = mod;
		this.mMapPath = mMapPath;
		this.synonymsPath = synonymsPath;
		this.tmapPath = tmapPath;
	}

	public void run() throws Exception {
		File vcfFile = preprocessVcf(new File(vcfPath));
		File outputFile = new File(outputPath);

		Gff3GeneModelBuilder gffBuilder = new Gff3GeneModelBuilder();
		GeneModel geneModel = gffBuilder.build(gffPath);
		if (tmapPath != null) {
			geneModel.applyTranscriptNameMap(tmapPath);
		}

		try (ReferenceGenome reference = new ReferenceGenome(fastaPath)) {

			ContigAccessionMap contigMap = ContigAccessionMap.fromFasta(fastaPath, synonymsPath);

			PredictionLookup siftLookup = null;
			PredictionLookup polyPhenLookup = null;
			if (mMapPath != null) {
				Path mMapDir = Path.of(mMapPath);
				try {
					siftLookup = new PredictionLookup(mMapDir, mod, "sift");
					log.info("Loaded SIFT predictions from {}", mMapDir);
				} catch (Exception e) {
					log.info("No SIFT predictions available for {}: {}", mod, e.getMessage());
				}
				try {
					polyPhenLookup = new PredictionLookup(mMapDir, mod, "pph");
					log.info("Loaded PolyPhen predictions from {}", mMapDir);
				} catch (Exception e) {
					log.info("No PolyPhen predictions available for {}: {}", mod, e.getMessage());
				}
			}

			OutputFactory annotator = new OutputFactory(geneModel, reference, contigMap, mod,
				siftLookup, polyPhenLookup);

			try (VCFFileReader reader = new VCFFileReader(vcfFile, false)) {
				VCFHeader header = reader.getFileHeader();
				CsqHeaderWriter.addHeaders(header, mod);

				ProcessDisplayHelper ph = new ProcessDisplayHelper(1000);

				VariantContextWriter writer = new VariantContextWriterBuilder()
					.setOutputFile(outputFile)
					.setReferenceDictionary(header.getSequenceDictionary())
					.unsetOption(Options.INDEX_ON_THE_FLY)
					.build();

				writer.writeHeader(header);

				Iterator<VariantContext> iterator = reader.iterator();
				ph.startProcess("VEP Annotation: " + mod);
				while (iterator.hasNext()) {
					VariantContext vc = iterator.next();

					// Normalize contig to GFF/FASTA canonical case (e.g., VCF chrMt → GFF chrmt).
					// Perl VEP does this via --fasta loading; match its behavior so downstream
					// diffs and index joins align on chromosome.
					String canonical = geneModel.normalizeContig(vc.getContig());
					if (!canonical.equals(vc.getContig())) {
						vc = new VariantContextBuilder(vc).chr(canonical).make();
					}

					List<CsqEntry> csqEntries = annotator.annotate(vc);
					VariantContext annotated = addCsq(vc, csqEntries);

					writer.add(annotated);
					ph.progressProcess();
				}

				writer.close();
				ph.finishProcess();
			}
		}
	}

	/**
	 * Preprocess a VCF file to fix issues that htsjdk rejects.
	 * Fixes: (1) Number=0 for non-Flag INFO/FORMAT fields (RGD VCF),
	 * (2) commas in REF allele field (HUMAN VCF — invalid per spec, VEP Perl skips these too).
	 * Returns the original file if no fixes needed, or a temp file if fixed.
	 */
	private File preprocessVcf(File vcfFile) throws Exception {
		boolean needsFix = false;

		try (BufferedReader br = openVcfReader(vcfFile)) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("##")) {
					if (line.contains("Number=0") && !line.contains("Type=Flag")) {
						needsFix = true;
						break;
					}
				} else if (!line.startsWith("#") && hasInvalidRef(line)) {
					needsFix = true;
					break;
				}
			}
		}

		if (!needsFix) return vcfFile;

		log.info("Preprocessing VCF to fix invalid records: {}", vcfFile.getName());
		File tempFile = File.createTempFile("vep_vcf_", ".vcf");
		tempFile.deleteOnExit();
		int skippedCount = 0;

		try (BufferedReader br = openVcfReader(vcfFile);
			 BufferedWriter bw = Files.newBufferedWriter(tempFile.toPath())) {

			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("##")) {
					if (line.contains("Number=0") && !line.contains("Type=Flag")) {
						line = line.replace("Number=0", "Number=.");
					}
				} else if (!line.startsWith("#") && hasInvalidRef(line)) {
					skippedCount++;
					continue;
				}
				bw.write(line);
				bw.newLine();
			}
		}

		if (skippedCount > 0) {
			log.info("Skipped {} records with invalid REF alleles (commas in REF field)", skippedCount);
		}
		return tempFile;
	}

	private BufferedReader openVcfReader(File vcfFile) throws Exception {
		if (vcfFile.getName().endsWith(".gz")) {
			return new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(vcfFile.toPath()))));
		}
		return Files.newBufferedReader(vcfFile.toPath());
	}

	private boolean hasInvalidRef(String line) {
		int tabs = 0;
		int refStart = 0;
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == '\t') {
				tabs++;
				if (tabs == 3) refStart = i + 1;
				if (tabs == 4) return line.substring(refStart, i).indexOf(',') >= 0;
			}
		}
		return false;
	}

	private VariantContext addCsq(VariantContext vc, List<CsqEntry> entries) {
		if (entries.isEmpty()) {
			return vc;
		}
		String csqValue = entries.stream()
			.map(CsqEntry::toVcfString)
			.collect(Collectors.joining(","));

		return new VariantContextBuilder(vc)
			.attribute("CSQ", csqValue)
			.make();
	}
}
