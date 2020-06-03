package org.alliancegenome.variant_indexer.config;

import static org.alliancegenome.core.config.Constants.*;

import java.nio.file.Path;
import java.util.Date;
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

        defaults.put(ES_INDEX, "site_variant_index"); // Can be over written
        defaults.put(ES_HOST, "localhost");
        defaults.put(ES_PORT, "9200");

        defaults.put(NEO4J_HOST, "localhost");
        defaults.put(NEO4J_PORT, "7687");

        defaults.put(VARIANT_CONFIG_FILE, "downloadFileSet.yaml");
        
        defaults.put(VARIANT_FILE_DOWNLOAD_THREADS, "10");
        defaults.put(VARIANT_FILE_DOWNLOAD_PATH, "data");

        defaults.put(VARIANT_DOCUMENT_CREATOR_THREADS, "4");
        defaults.put(VARIANT_DOCUMENT_CREATOR_CONTEXT_PROCESSOR_TASK_THREADS, "6");
        defaults.put(VARIANT_DOCUMENT_CREATOR_CONTEXT_PROCESSOR_TASK_QUEUE_SIZE, "100");
        defaults.put(VARIANT_DOCUMENT_CREATOR_DOCUMENT_WRITER_TASK_THREADS, "1");
        defaults.put(VARIANT_DOCUMENT_CREATOR_DOCUMENT_WRITER_TASK_QUEUE_SIZE, "10000");
        defaults.put(VARIANT_DOCUMENT_CREATOR_WORK_CHUNK_SIZE, "100");
        
        defaults.put(VARIANT_DOCUMENT_INDEXER_THREADS, "1");
        defaults.put(VARIANT_ES_BULK_REQUEST_QUEUE_SIZE, "10000");
        defaults.put(VARIANT_ES_BULK_ACTION_SIZE, "250000");
        defaults.put(VARIANT_ES_BULK_CONCURRENT_REQUEST_AMOUNT, "3");
        defaults.put(VARIANT_ES_BULK_SIZE_MB, "100");
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

    public static String getEsHost() {
        if (!init) init();
        return config.get(ES_HOST);
    }

    public static int getEsPort() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(ES_PORT));
        } catch (NumberFormatException e) {
            return 9300;
        }
    }

    public static String getNeo4jHost() {
        if (!init) init();
        return config.get(NEO4J_HOST);
    }

    public static int getNeo4jPort() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(NEO4J_PORT));
        } catch (NumberFormatException e) {
            return 7687;
        }
    }

    public static String getEsIndex() {
        if (!init) init();
        return config.get(ES_INDEX);
    }

    public static String getVariantConfigFile() {
        if (!init) init();
        return config.get(VARIANT_CONFIG_FILE);
    }

    public static String getVariantFileDownloadPath() {
        if (!init) init();
        return config.get(VARIANT_FILE_DOWNLOAD_PATH);
    }

    public static String getJavaLineSeparator() {
        if (!init) init();
        return System.getProperty("line.separator");
    }

    public static String getJavaTmpDir() {
        if (!init) init();
        return System.getProperty("java.io.tmpdir");
    }

    public static String getValidationSoftwarePath() {
        if (!init) init();
        return getJavaTmpDir();
    }

    public static void setNameValue(String key, String value) {
        config.put(key, value);
    }

    public static void printProperties() {
        log.info("Running with Properties:");
        for (String key : allKeys) {
            log.info("\t" + key + ": " + config.get(key));
        }
    }

    public static Integer getEsBulkRequestQueueSize() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_ES_BULK_REQUEST_QUEUE_SIZE));
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

    public static Integer getDocumentIndexerThreads() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_DOCUMENT_INDEXER_THREADS));
        } catch (NumberFormatException e) {
            return 1;
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

    public static Integer getDocumentWriterTaskThreads() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_DOCUMENT_CREATOR_DOCUMENT_WRITER_TASK_THREADS));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public static Integer getDocumentWriterTaskQueueSize() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(VARIANT_DOCUMENT_CREATOR_DOCUMENT_WRITER_TASK_QUEUE_SIZE));
        } catch (NumberFormatException e) {
            return 10000;
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



}
