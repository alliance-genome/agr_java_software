package org.alliancegenome.vep;

import java.io.InputStream;

import java.nio.file.Path;

import org.alliancegenome.vep.annotation.OutputFactory;
import org.alliancegenome.vep.config.VepFileSet;
import org.alliancegenome.vep.config.VepFileSet.ModSource;
import org.alliancegenome.vep.gff.Gff3GeneModelBuilder;
import org.alliancegenome.vep.model.GeneModel;
import org.alliancegenome.vep.plugin.PredictionLookup;
import org.alliancegenome.vep.reference.ContigAccessionMap;
import org.alliancegenome.vep.reference.ReferenceGenome;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Shared test utilities. Loads VepFileSet from classpath and creates annotators per MOD.
 */
public class TestHelper {

	private static VepFileSet fileSet;

	public static synchronized VepFileSet getFileSet() throws Exception {
		if (fileSet == null) {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			InputStream is = TestHelper.class.getClassLoader().getResourceAsStream("VepFileSet.yaml");
			fileSet = mapper.readValue(is, VepFileSet.class);
			fileSet.init();
		}
		return fileSet;
	}

	public static ModSource getMod(String modName) throws Exception {
		for (ModSource mod : getFileSet().getModSources()) {
			if (mod.getMod().equalsIgnoreCase(modName)) {
				return mod;
			}
		}
		throw new IllegalArgumentException("MOD not found: " + modName);
	}

	public static OutputFactory createAnnotator(String modName) throws Exception {
		ModSource mod = getMod(modName);
		Gff3GeneModelBuilder builder = new Gff3GeneModelBuilder();
		GeneModel geneModel = builder.build(mod.getGffFilePath());
		geneModel.applyTranscriptNameMap(mod.getTranscriptMapFilePath());
		ReferenceGenome reference = new ReferenceGenome(mod.getFastaFilePath());
		ContigAccessionMap contigMap = ContigAccessionMap.fromFasta(
			mod.getFastaFilePath(), mod.getSynonymsFilePath());
		PredictionLookup siftLookup = null;
		PredictionLookup polyPhenLookup = null;
		String mmapPath = mod.getMmapPath();
		if (mmapPath != null) {
			Path mmapDir = Path.of(mmapPath);
			try { siftLookup = new PredictionLookup(mmapDir, modName, "sift"); } catch (Exception e) { /* skip */ }
			try { polyPhenLookup = new PredictionLookup(mmapDir, modName, "pph"); } catch (Exception e) { /* skip */ }
		}
		return new OutputFactory(geneModel, reference, contigMap, modName,
			siftLookup, polyPhenLookup);
	}

	public static GeneModel loadGeneModel(String modName) throws Exception {
		ModSource mod = getMod(modName);
		return new Gff3GeneModelBuilder().build(mod.getGffFilePath());
	}

	public static String getTestResourceDir() {
		return "src/test/resources";
	}
}
