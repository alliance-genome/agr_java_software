package org.alliancegenome.vep.config;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VepFileSet {

	private String rootPath;
	private String outputPath;
	private String gffPath;
	private String inputPath;
	private String fastaPath;
	private String bamPath;
	private String mmapPath;
	private String synonymsPath;
	private String tmapPath;
	private List<ModSource> modSources;

	public void init() {
		if (modSources != null) {
			for (ModSource mod : modSources) {
				mod.setFileSet(this);
			}
		}
	}

	@Getter
	@Setter
	public static class TestFile {
		private String consequence;
		private String vcfFile;
	}

	@Getter
	@Setter
	public static class ModSource {
		private String mod;
		private boolean active;
		private String vcfFile;
		private String gffFile;
		private String fastaFile;
		private String bamFile;
		private String synonymsFile;
		private String transcriptMapFile;
		private List<TestFile> testFiles;

		private transient VepFileSet fileSet;

		public String getVcfFilePath() {
			return fileSet.getRootPath() + "/" + fileSet.getInputPath() + "/" + vcfFile;
		}

		public String getGffFilePath() {
			return fileSet.getRootPath() + "/" + fileSet.getGffPath() + "/" + gffFile;
		}

		public String getFastaFilePath() {
			return fileSet.getRootPath() + "/" + fileSet.getFastaPath() + "/" + fastaFile;
		}

		public String getBamFilePath() {
			if (bamFile == null || bamFile.isEmpty()) return null;
			return fileSet.getRootPath() + "/" + fileSet.getBamPath() + "/" + bamFile;
		}

		public String getOutputFilePath() {
			return fileSet.getRootPath() + "/" + fileSet.getOutputPath() + "/" + mod + ".vep.vcf.gz";
		}

		public String getMmapPath() {
			if (fileSet.getMmapPath() == null || fileSet.getMmapPath().isEmpty()) return null;
			return fileSet.getRootPath() + "/" + fileSet.getMmapPath();
		}

		public String getSynonymsFilePath() {
			if (synonymsFile == null || synonymsFile.isEmpty()) return null;
			if (fileSet.getSynonymsPath() == null || fileSet.getSynonymsPath().isEmpty()) return null;
			return fileSet.getRootPath() + "/" + fileSet.getSynonymsPath() + "/" + synonymsFile;
		}

		public String getTranscriptMapFilePath() {
			if (transcriptMapFile == null || transcriptMapFile.isEmpty()) return null;
			if (fileSet.getTmapPath() == null || fileSet.getTmapPath().isEmpty()) return null;
			return fileSet.getRootPath() + "/" + fileSet.getTmapPath() + "/" + transcriptMapFile;
		}
	}
}
