package org.alliancegenome.vep;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.vep.config.VepFileSet;
import org.alliancegenome.vep.config.VepFileSet.ModSource;
import org.alliancegenome.vep.vcf.VcfAnnotationPipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {

	private Main() {
	}

	public static void main(String[] args) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

		try {
			VepFileSet fileSet;

			String configPath = null;
			List<String> onlyMods = null;
			for (int i = 0; i < args.length; i++) {
				if ("--config".equals(args[i]) && i + 1 < args.length) {
					configPath = args[++i];
				} else if ("--mod".equals(args[i]) && i + 1 < args.length) {
					onlyMods = new ArrayList<>();
					for (String m : args[++i].split(",")) {
						onlyMods.add(m.trim().toUpperCase());
					}
				}
			}

			if (configPath != null) {
				log.info("Loading config from: {}", configPath);
				fileSet = mapper.readValue(new File(configPath), VepFileSet.class);
			} else {
				log.info("Loading default config from classpath: VepFileSet.yaml");
				InputStream is = Main.class.getClassLoader().getResourceAsStream("VepFileSet.yaml");
				if (is == null) {
					log.error("VepFileSet.yaml not found on classpath");
					System.exit(1);
					return;
				}
				fileSet = mapper.readValue(is, VepFileSet.class);
			}

			fileSet.init();

			log.info("AGR VEP starting");
			log.info("Root path: {}", fileSet.getRootPath());

			ArrayList<VcfAnnotationPipeline> pipelines = new ArrayList<VcfAnnotationPipeline>();
			
			for (ModSource mod : fileSet.getModSources()) {
				if (onlyMods != null && !onlyMods.contains(mod.getMod().toUpperCase())) {
					continue;
				}
				if (!mod.isActive()) {
					log.info("Skipping inactive MOD: {}", mod.getMod());
					continue;
				}
				if (mod.getVcfFile() == null || mod.getVcfFile().isEmpty()) {
					log.info("Skipping MOD with no VCF file: {}", mod.getMod());
					continue;
				}

				log.info("Processing MOD: {}", mod.getMod());
				log.info("  VCF:    {}", mod.getVcfFilePath());
				log.info("  GFF:    {}", mod.getGffFilePath());
				log.info("  FASTA:  {}", mod.getFastaFilePath());
				if (mod.getBamFilePath() != null) {
					log.info("  BAM:    {}", mod.getBamFilePath());
				}
				log.info("  Output: {}", mod.getOutputFilePath());

				VcfAnnotationPipeline pipeline = new VcfAnnotationPipeline(mod.getVcfFilePath(), mod.getGffFilePath(), mod.getFastaFilePath(), mod.getBamFilePath(), mod.getOutputFilePath(), mod.getMod(), mod.getMmapPath(), mod.getSynonymsFilePath(), mod.getTranscriptMapFilePath());
				pipeline.start();
				pipelines.add(pipeline);

				log.info("Completed MOD: {}", mod.getMod());
			}
			
			for(VcfAnnotationPipeline pipeline: pipelines) {
				pipeline.join();
			}

			log.info("AGR VEP completed successfully");

		} catch (Exception e) {
			log.error("AGR VEP failed", e);
			System.exit(1);
		}
	}
}
