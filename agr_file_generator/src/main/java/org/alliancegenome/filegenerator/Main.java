package org.alliancegenome.filegenerator;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.filegenerator.config.FileGeneratorConfig;
import org.alliancegenome.filegenerator.generators.FileGenerator;
import org.alliancegenome.filegenerator.s3.S3Uploader;
import org.apache.commons.lang3.time.DurationFormatUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

	private Main() {
	}

	public static void main(String[] args) {
		ConfigHelper.init();
		ExceptionCatcher.initialize();

		ProcessDisplayHelper ph = new ProcessDisplayHelper();
		ph.startProcess("File Generator Main: ");

		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			log.error("Thread: " + t.threadId() + " has uncaught exceptions");
			e.printStackTrace();
			System.exit(-1);
		});

		Set<String> argumentSet = new HashSet<>();
		for (int i = 0; i < args.length; i++) {
			argumentSet.add(args[i]);
			log.info("Args[" + i + "]: " + args[i]);
		}

		Map<String, FileGenerator> generators = new LinkedHashMap<>();

		for (FileGeneratorConfig fc : FileGeneratorConfig.values()) {
			if (!argumentSet.isEmpty() && !argumentSet.contains(fc.name())) {
				log.info("Skipping: " + fc.name());
				continue;
			}
			try {
				FileGenerator g = fc.getGeneratorClazz().getDeclaredConstructor(FileGeneratorConfig.class).newInstance(fc);
				generators.put(fc.name(), g);
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e.getMessage());
				ExceptionCatcher.report(e);
				System.exit(-1);
			}
		}

		// Each FileGenerator is itself a Thread — start() forks, run() would block. We want
		// every generator scrolling its own ES category in parallel; the cluster can absorb the
		// concurrent load (each generator's scroll is sliced 8 ways internally).
		for (Entry<String, FileGenerator> entry : generators.entrySet()) {
			log.info("Starting generator: " + entry.getKey());
			entry.getValue().start();
		}
		for (Entry<String, FileGenerator> entry : generators.entrySet()) {
			try {
				entry.getValue().join();
				String elapsed = DurationFormatUtils.formatDuration(entry.getValue().getDuration().toMillis(), "HH:mm:ss", true);
				log.info("Generator: " + entry.getKey() + " finished in " + elapsed);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.error("Interrupted while waiting for generator: " + entry.getKey());
				break;
			}
		}

		// Upload generated files to S3 unless explicitly disabled. Requires ALLIANCE_RELEASE.
		// Honors SKIP_S3_UPLOAD=true for local / dev runs.
		if (!ConfigHelper.getSkipS3Upload()) {
			try {
				S3Uploader.uploadDirectory(Paths.get(ConfigHelper.getGeneratedFilesFolder()));
			} catch (Exception e) {
				log.error("S3 upload failed: " + e.getMessage(), e);
				ExceptionCatcher.report(e);
				// Don't exit non-zero — files are written, only the upload failed.
			}
		} else {
			log.info("SKIP_S3_UPLOAD=true — skipping S3 upload");
		}

		ph.finishProcess();
	}

}
