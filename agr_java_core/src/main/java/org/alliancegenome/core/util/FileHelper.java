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

import lombok.extern.log4j.Log4j2;

@Log4j2
public class FileHelper {

	private static Map<String, Map<String, Boolean>> applicabilityMatrix = null;
	
	public static Map<String, Map<String, Boolean>> getApplicabilityMatrix(String ribbonTermSpeciesApplicabilityPath) {
		Map<String, Map<String, Boolean>> applicabilityMatrix = new HashMap<>();
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
				applicabilityMatrix.computeIfAbsent(speciesName, k -> new LinkedHashMap<>());
				Map<String, Boolean> speciesMap = applicabilityMatrix.get(speciesName);
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
		return applicabilityMatrix;
	}

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

	public static LinkedHashMap<String, String> getNameValuePairsList(String filePath) {

		LinkedHashMap<String, String> nameValueList = new LinkedHashMap<>();
		InputStream in = null;
		BufferedReader reader = null;
		try {
			String str = null;
			in = FileHelper.class.getClassLoader().getResourceAsStream(filePath);
			if (in != null) {
				reader = new BufferedReader(new InputStreamReader(in));
				while ((str = reader.readLine()) != null) {
					String[] token = str.split("\t");
					if (token.length < 2) {
						final String message = "Could not find two columns in ordering file: " + filePath;
						log.error(message);
						throw new RuntimeException(message);
					}
					nameValueList.put(token[0], token[1]);
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

		return nameValueList;
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


	public static LinkedHashMap<String, String> getAOTermList() {
		return FileHelper.getNameValuePairsList(ConfigHelper.getAOTermListFilePath());
	}

	public static LinkedHashMap<String, String> getGOTermList() {
		return FileHelper.getNameValuePairsList(ConfigHelper.getGOTermListFilePath());
	}

	public static Map<String, Map<String, Boolean>> getRibbonTermSpeciesApplicabilityMap() {
		return getMapFromCSVFile();
	}
	
	private static Map<String, Map<String, Boolean>> getMapFromCSVFile() {
		// cache the applicability matrix
		if (applicabilityMatrix != null)
			return applicabilityMatrix;

		String ribbonTermSpeciesApplicabilityPath = ConfigHelper.getRibbonTermSpeciesApplicabilityPath();
		applicabilityMatrix = FileHelper.getApplicabilityMatrix(ribbonTermSpeciesApplicabilityPath);
		return applicabilityMatrix;
	}
	
	public static Boolean getRibbonTermSpeciesApplicability(String id, String displayName) {
		Map<String, Boolean> map = getRibbonTermSpeciesApplicabilityMap().get(displayName);
		if (map == null) {
			log.error("Could not find applicability matrix for species with mod name " + displayName);
			return false;
		}
		return map.get(id);
	}

}
