package org.alliancegenome.variant_indexer.config;

import static org.alliancegenome.core.config.Constants.VARIANT_CONFIG_FILE;
import static org.alliancegenome.core.config.Constants.VARIANT_DOCUMENT_CREATOR_CONTEXT_PROCESSOR_TASK_QUEUE_SIZE;
import static org.alliancegenome.core.config.Constants.VARIANT_DOCUMENT_CREATOR_CONTEXT_PROCESSOR_TASK_THREADS;
import static org.alliancegenome.core.config.Constants.VARIANT_DOCUMENT_CREATOR_THREADS;
import static org.alliancegenome.core.config.Constants.VARIANT_DOCUMENT_CREATOR_WORK_CHUNK_SIZE;
import static org.alliancegenome.core.config.Constants.VARIANT_ES_BULK_ACTION_SIZE;
import static org.alliancegenome.core.config.Constants.VARIANT_ES_BULK_CONCURRENT_REQUEST_AMOUNT;
import static org.alliancegenome.core.config.Constants.VARIANT_ES_BULK_SIZE_MB;
import static org.alliancegenome.core.config.Constants.VARIANT_ES_INDEX_NUMBER_OF_SHARDS;
import static org.alliancegenome.core.config.Constants.VARIANT_FILE_DOWNLOAD_FILTER_THREADS;
import static org.alliancegenome.core.config.Constants.VARIANT_FILE_DOWNLOAD_PATH;
import static org.alliancegenome.core.config.Constants.VARIANT_FILE_DOWNLOAD_THREADS;
import static org.alliancegenome.core.config.Constants.VARIANT_INDEX_REQUEST_QUEUE_SIZE;

import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import org.alliancegenome.core.util.FileHelper;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class VariantConfigHelper {

    private static Properties configProperties = new Properties();

    private static HashMap<String, String> defaults = new HashMap<>();
    private static HashMap<String, String> config = new HashMap<>();
    private static Set<String> allKeys;
    private static boolean init = false;

    public VariantConfigHelper() {
        init();
    }

    public static void init() {
        /* The purpose of the default values is that these are the values required by the application to run
         * This config help looks at 3 methods to over write these values.
         * 1. Via System Properties with are passed to the application at run time via the -DNAME=value
         * 2. Via a config.properties file, this file only need to over write the values it wants to change from the defaults
         * otherwise the config will be loaded from the default values.
         * 3. Via the environment. Before the application is run export NAME=value in the shell the config helper will load the
         * NAME and value that it finds in the environments and use those values rather then the defaults.
         * The names and values need to not be changed as other things might depend on the defaulted values for executing. For each
         * key in the defaults map there should be a corresponding get method for that value.
         */

        defaults.put(VARIANT_CONFIG_FILE, "downloadFileSet.yaml");
        
        defaults.put(VARIANT_FILE_DOWNLOAD_THREADS, "10");
        defaults.put(VARIANT_FILE_DOWNLOAD_FILTER_THREADS, "10");
        defaults.put(VARIANT_FILE_DOWNLOAD_PATH, "data");

        defaults.put(VARIANT_DOCUMENT_CREATOR_THREADS, "10");
        defaults.put(VARIANT_DOCUMENT_CREATOR_CONTEXT_PROCESSOR_TASK_THREADS, "8");
        defaults.put(VARIANT_DOCUMENT_CREATOR_CONTEXT_PROCESSOR_TASK_QUEUE_SIZE, "10");
        defaults.put(VARIANT_DOCUMENT_CREATOR_WORK_CHUNK_SIZE, "8000");

        // Average document size is 1200b
        defaults.put(VARIANT_INDEX_REQUEST_QUEUE_SIZE, "1000"); // Queue documents waiting to go into a (target * 10) MB ES bulk request
        defaults.put(VARIANT_ES_BULK_ACTION_SIZE, "500"); // Max amount of documents in a bulk request target MB
        defaults.put(VARIANT_ES_BULK_CONCURRENT_REQUEST_AMOUNT, "2"); // Amount of concurrent bulk requests running (target * 10) MB
        defaults.put(VARIANT_ES_BULK_SIZE_MB, "7"); // Max size of bulk request target
        defaults.put(VARIANT_ES_INDEX_NUMBER_OF_SHARDS, "8");

        allKeys = defaults.keySet();

        if (configProperties.size() == 0) {
            final String configPropertiesFileName = "config.properties";
            configProperties = FileHelper.getPropertiesFromFile(configPropertiesFileName);
        }

        for (String key : allKeys) {
            // First checks the -D params and sets config[key] = value otherwise it will be null.
            if (config.get(key) == null) config.put(key, loadSystemProperty(key));
            // Second checks the config.properties file built into the application otherwise it will be null.
            if (config.get(key) == null) config.put(key, loadConfigProperty(key));
            // Third checks the environment for a NAME = value otherwise leaves it null.
            if (config.get(key) == null) config.put(key, loadSystemENVProperty(key));
            // Lastly loads the default value for NAME = value and loadDefaultProperty ensures it won't be null.
            if (config.get(key) == null) config.put(key, loadDefaultProperty(key));
        }
        printProperties();
        init = true;
    }

    private static String loadSystemProperty(String key) {
        String ret = System.getProperty(key);
        if (ret != null) log.debug("Found: -D " + key + "=" + ret);
        return ret;
    }

    private static String loadConfigProperty(String key) {
        String ret = configProperties.getProperty(key);
        if (ret != null) log.debug("Config File Property: " + key + "=" + ret);
        return ret;
    }

    private static String loadSystemENVProperty(String key) {
        String ret = System.getenv(key);
        if (ret != null) log.debug("Found Enviroment ENV[" + key + "]=" + ret);
        return ret;
    }

    private static String loadDefaultProperty(String key) {
        String ret = defaults.get(key);
        if (ret != null) log.debug("Setting default: " + key + "=" + ret);
        return ret;
    }

    public static String getVariantConfigFile() {
        if (!init) init();
        return config.get(VARIANT_CONFIG_FILE);
    }

    public static String getVariantFileDownloadPath() {
        if (!init) init();
        return config.get(VARIANT_FILE_DOWNLOAD_PATH);
    }

    public static void printProperties() {
        log.info("Running with Properties:");
        for (String key : allKeys) {
            log.info("\t" + key + ": " + config.get(key));
        }
    }

    public static Integer getIndexRequestQueueSize() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_INDEX_REQUEST_QUEUE_SIZE));
        } catch (NumberFormatException e) {
            return 10000;
        }
    }

    public static Integer getEsBulkActionSize() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_ES_BULK_ACTION_SIZE));
        } catch (NumberFormatException e) {
            return 250000;
        }
    }

    public static Integer getEsBulkConcurrentRequestsAmount() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_ES_BULK_CONCURRENT_REQUEST_AMOUNT));
        } catch (NumberFormatException e) {
            return 3;
        }
    }

    public static Integer getEsBulkSizeMB() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_ES_BULK_SIZE_MB));
        } catch (NumberFormatException e) {
            return 100;
        }
    }

    public static Integer getEsNumberOfShards() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_ES_INDEX_NUMBER_OF_SHARDS));
        } catch (NumberFormatException e) {
            return 8;
        }
    }

    public static Integer getFileDownloadThreads() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_FILE_DOWNLOAD_THREADS));
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    public static Integer getDocumentCreatorThreads() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_DOCUMENT_CREATOR_THREADS));
        } catch (NumberFormatException e) {
            return 4;
        }
    }

    public static Integer getContextProcessorTaskThreads() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_DOCUMENT_CREATOR_CONTEXT_PROCESSOR_TASK_THREADS));
        } catch (NumberFormatException e) {
            return 6;
        }
    }

    public static Integer getContextProcessorTaskQueueSize() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_DOCUMENT_CREATOR_CONTEXT_PROCESSOR_TASK_QUEUE_SIZE));
        } catch (NumberFormatException e) {
            return 100;
        }
    }

    public static Integer getDocumentCreatorWorkChunkSize() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_DOCUMENT_CREATOR_WORK_CHUNK_SIZE));
        } catch (NumberFormatException e) {
            return 100;
        }
    }

    public static int getFileDownloadFilterThreads() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_FILE_DOWNLOAD_FILTER_THREADS));
        } catch (NumberFormatException e) {
            return 10;
        }
    }



}
