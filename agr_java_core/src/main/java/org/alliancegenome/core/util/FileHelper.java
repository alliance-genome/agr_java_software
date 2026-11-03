package org.alliancegenome.core.util;

import static org.alliancegenome.core.config.ConfigHelper.getJavaLineSeparator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alliancegenome.core.config.ConfigHelper;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileHelper {

	private FileHelper() { }

	private static Map<String, Map<String, Boolean>> applicabilityMatrix;

	public static String getFileContent(String filePath) {

		InputStream in = null;
		BufferedReader reader = null;
		String result = "";
		try {
			String str = null;
			in = FileHelper.class.getClassLoader().getResourceAsStream(filePath);
			if (in != null) {
				reader = new BufferedReader(new InputStreamReader(in));
				while ((str = reader.readLine()) != null) {
					result += str + getJavaLineSeparator();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	public static Properties getPropertiesFromFile(String configPropertiesFileName) {
		Properties configProperties = new Properties();
		InputStream in = FileHelper.class.getClassLoader().getResourceAsStream(configPropertiesFileName);
		if (in == null) {
			log.debug("No config.properties file, other config options will be used");
		} else {
			try {
				configProperties.load(in);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return configProperties;
	}

	private static Map<String, Map<String, Boolean>> getMapFromCSVFile() {
		// cache the applicability matrix
		if (applicabilityMatrix != null) {
			return applicabilityMatrix;
		}

		String ribbonTermSpeciesApplicabilityPath = ConfigHelper.getRibbonTermSpeciesApplicabilityPath();
		Map<String, Map<String, Boolean>> matrix = new HashMap<>();
		InputStream in = null;
		try {
			in = FileHelper.class.getClassLoader().getResourceAsStream(ribbonTermSpeciesApplicabilityPath);
			CSVReader reader = new CSVReader(new InputStreamReader(in));
			List<String[]> values = reader.readAll();

			String[] headerSpeciesNames = values.get(0);
			Map<Integer, String> speciesColumnMapping = new HashMap<>();
			for (int index = 1; index < headerSpeciesNames.length; index++) {
				speciesColumnMapping.put(index, headerSpeciesNames[index]);
			}

			values.remove(0);
			speciesColumnMapping.forEach((index, speciesName) -> {
				matrix.computeIfAbsent(speciesName, k -> new LinkedHashMap<>());
				Map<String, Boolean> speciesMap = matrix.get(speciesName);
				values.forEach(line -> speciesMap.put(line[0], Boolean.valueOf(line[index])));
			});
		} catch (IOException | CsvException e) {
			log.error("error while reading applicability matrix in file " + ribbonTermSpeciesApplicabilityPath, e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error("error closing input stream", e);
				}
			}
		}
		applicabilityMatrix = matrix;
		return applicabilityMatrix;
	}

	public static Boolean getRibbonTermSpeciesApplicability(String id, String displayName) {
		Map<String, Boolean> map = getMapFromCSVFile().get(displayName);
		if (map == null) {
			log.error("Could not find applicability matrix for species with mod name " + displayName);
			return false;
		}
		return map.get(id);
	}
}
