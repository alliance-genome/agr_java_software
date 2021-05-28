package org.alliancegenome.core.config;

public class Constants {

    
    // Config Helper Constants
    public static final String THREADED = "THREADED";
    public static final String DEBUG = "DEBUG";
    
    public static final String ES_INDEX = "ES_INDEX";
    public static final String ES_INDEX_PREFIX = "ES_INDEX_PREFIX";
    public static final String ES_INDEX_SUFFIX = "ES_INDEX_SUFFIX";
    public static final String ES_HOST = "ES_HOST";
    public static final String ES_PORT = "ES_PORT";
    
    public static final String ES_BULK_ACTION_SIZE = "ES_BULK_ACTION_SIZE";
    public static final String ES_BULK_REQUEST_SIZE = "ES_BULK_REQUEST_SIZE";
    public static final String ES_BULK_CONCURRENT_REQUESTS = "ES_BULK_CONCURRENT_REQUESTS";

    public static final String INDEX_VARIANTS = "INDEX_VARIANTS";

    public static final String API_HOST = "API_HOST";
    public static final String API_PORT = "API_PORT";
    public static final String API_SECURE = "API_SECURE";

    public static final String CACHE_HOST = "CACHE_HOST";
    public static final String CACHE_PORT = "CACHE_PORT";

    public static final String NEO4J_HOST = "NEO4J_HOST";
    public static final String NEO4J_PORT = "NEO4J_PORT";

    public static final String AWS_BUCKET_NAME = "AWS_BUCKET_NAME";

    public static final String AO_TERM_LIST = "AO_TERM_LIST";
    public static final String GO_TERM_LIST = "GO_TERM_LIST";
    public static final String VARIANT_DOWNLOAD_PATH = "VARIANT_DOWNLOAD_PATH";
    public static final String RIBBON_TERM_SPECIES_APPLICABILITY = "RIBBON_TERM_SPECIES_APPLICABILITY";
    public static final String POPULARITY_DOWNLOAD_URL = "POPULARITY_DOWNLOAD_URL";
    public static final String POPULARITY_FILE_NAME = "POPULARITY_FILE_NAME";
    public static final String EXTRACTOR_OUTPUTDIR = "EXTRACTOR_OUTPUTDIR";

    // Other Constants
    public static final String SEARCHABLE_ITEM = "searchable_item";
    
    // Variant Indexer Related ENV param constants
    public static final String VARIANT_MOD_DOWNLOAD_SET_FILE = "VARIANT_MOD_DOWNLOAD_SET_FILE";
    public static final String VARIANT_HUMAN_DOWNLOAD_SET_FILE = "VARIANT_HUMAN_DOWNLOAD_SET_FILE";
    
    public static final String VARIANTS_TO_INDEX = "VARIANTS_TO_INDEX";
    
    public static final String VARIANT_CACHER_CONFIG_FILE = "VARIANT_CACHER_CONFIG_FILE";
    public static final String VARIANT_CONFIG_DOWNLOAD = "VARIANT_CONFIG_DOWNLOAD";
    public static final String VARIANT_CONFIG_CREATING = "VARIANT_CONFIG_CREATING";
    public static final String VARIANT_CONFIG_INDEXING = "VARIANT_CONFIG_INDEXING";
    public static final String VARIANT_CONFIG_GATHERSTATS = "VARIANT_CONFIG_GATHERSTATS";
    public static final String FMS_URL = "FMS_URL";
    public static final String ALLIANCE_RELEASE = "ALLIANCE_RELEASE";
    
    // Variant File Downloader
    public static final String VARIANT_FILE_DOWNLOAD_THREADS = "VARIANT_FILE_DOWNLOAD_THREADS";
    public static final String VARIANT_FILE_DOWNLOAD_FILTER_THREADS = "VARIANT_FILE_DOWNLOAD_FILTER_THREADS";
    public static final String VARIANT_FILE_DOWNLOAD_PATH = "VARIANT_FILE_DOWNLOAD_PATH";
    
    // Variant Document Creator
    public static final String VARIANT_DISPLAY_INTERVAL = "VARIANT_DISPLAY_INTERVAL";
    public static final String VARIANT_SOURCE_DOCUMENT_CREATOR_THREADS = "VARIANT_SOURCE_DOCUMENT_CREATOR_THREADS";
    public static final String VARIANT_SOURCE_DOCUMENT_CREATOR_VCQUEUE_SIZE = "VARIANT_SOURCE_DOCUMENT_CREATOR_VCQUEUE_SIZE";
    public static final String VARIANT_SOURCE_DOCUMENT_CREATOR_VCQUEUE_BUCKET_SIZE = "VARIANT_SOURCE_DOCUMENT_CREATOR_VCQUEUE_BUCKET_SIZE";
    public static final String VARIANT_SOURCE_DOCUMENT_CREATOR_OBJECT_QUEUE_SIZE = "VARIANT_SOURCE_DOCUMENT_CREATOR_OBJECT_QUEUE_SIZE";
    public static final String VARIANT_SOURCE_DOCUMENT_CREATOR_OBJECT_QUEUE_BUCKET_SIZE = "VARIANT_SOURCE_DOCUMENT_CREATOR_OBJECT_QUEUE_BUCKET_SIZE";
    
    // Threads
    public static final String VARIANT_TRANSFORMER_THREADS = "VARIANT_TRANSFORMER_THREADS";
    public static final String VARIANT_PRODUCER_THREADS = "VARIANT_PRODUCER_THREADS";

    // Variant Indexer 
    public static final String VARIANT_INDEXER_SHARDS = "VARIANT_INDEXER_SHARDS";
    public static final String VARIANT_INDEXER_BULK_PROCESSOR_THREADS = "VARIANT_INDEXER_BULK_PROCESSOR_THREADS";
    public static final String VARIANT_BULK_PROCESSOR_SETTINGS = "VARIANT_BULK_PROCESSOR_SETTINGS";

}
