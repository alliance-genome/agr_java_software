package org.alliancegenome.core.config;

public class Constants {

    
    // Config Helper Constants
    public static final String THREADED = "THREADED";
    public static final String DEBUG = "DEBUG";
    
    public static final String ES_INDEX = "ES_INDEX";
    public static final String ES_INDEX_SUFFIX = "ES_INDEX_SUFFIX";
    public static final String ES_DATA_INDEX = "ES_DATA_INDEX";
    public static final String ES_HOST = "ES_HOST";
    public static final String ES_PORT = "ES_PORT";

    public static final String KEEPINDEX = "KEEPINDEX";
    public static final String SPECIES = "SPECIES";
    public static final String INDEX_VARIANTS = "INDEX_VARIANTS";

    public static final String API_HOST = "API_HOST";
    public static final String API_PORT = "API_PORT";
    public static final String API_SECURE = "API_SECURE";

    public static final String CACHE_HOST = "CACHE_HOST";
    public static final String CACHE_PORT = "CACHE_PORT";

    public static final String NEO4J_HOST = "NEO4J_HOST";
    public static final String NEO4J_PORT = "NEO4J_PORT";

    public static final String AWS_ACCESS_KEY = "AWS_ACCESS_KEY";
    public static final String AWS_SECRET_KEY = "AWS_SECRET_KEY";
    public static final String AWS_BUCKET_NAME = "AWS_BUCKET_NAME";

    public static final String AO_TERM_LIST = "AO_TERM_LIST";
    public static final String GO_TERM_LIST = "GO_TERM_LIST";
    public static final String RIBBON_TERM_SPECIES_APPLICABILITY = "RIBBON_TERM_SPECIES_APPLICABILITY";
    public static final String EXTRACTOR_OUTPUTDIR = "EXTRACTOR_OUTPUTDIR";

    // Other Constants
    public static final String SEARCHABLE_ITEM = "searchable_item";
    
    // Variant Indexer Related ENV param constants
    public static final String VARIANT_CONFIG_FILE = "VARIANT_CONFIG_FILE";
    // Variant File Downloader
    public static final String VARIANT_FILE_DOWNLOAD_THREADS = "VARIANT_FILE_DOWNLOAD_THREADS";
    public static final String VARIANT_FILE_DOWNLOAD_FILTER_THREADS = "VARIANT_FILE_DOWNLOAD_FILTER_THREADS";
    public static final String VARIANT_FILE_DOWNLOAD_PATH = "VARIANT_FILE_DOWNLOAD_PATH";
    // Variant Document Creator
    public static final String VARIANT_DOCUMENT_CREATOR_THREADS = "VARIANT_DOCUMENT_CREATOR_THREADS";
    public static final String VARIANT_DOCUMENT_CREATOR_CONTEXT_PROCESSOR_TASK_THREADS = "VARIANT_DOCUMENT_CREATOR_CONTEXT_PROCESSOR_TASK_THREADS";
    public static final String VARIANT_DOCUMENT_CREATOR_CONTEXT_PROCESSOR_TASK_QUEUE_SIZE = "VARIANT_DOCUMENT_CREATOR_CONTEXT_PROCESSOR_TASK_QUEUE_SIZE";
    public static final String VARIANT_DOCUMENT_CREATOR_DOCUMENT_WRITER_TASK_THREADS = "VARIANT_DOCUMENT_CREATOR_DOCUMENT_WRITER_TASK_THREADS";
    public static final String VARIANT_DOCUMENT_CREATOR_DOCUMENT_WRITER_TASK_QUEUE_SIZE = "VARIANT_DOCUMENT_CREATOR_DOCUMENT_WRITER_TASK_QUEUE_SIZE";
    public static final String VARIANT_DOCUMENT_CREATOR_WORK_CHUNK_SIZE = "VARIANT_DOCUMENT_CREATOR_WORK_CHUNK_SIZE";
    // Variant Indexer 
    public static final String VARIANT_DOCUMENT_INDEXER_THREADS = "VARIANT_DOCUMENT_INDEXER_THREADS";
    public static final String VARIANT_ES_BULK_REQUEST_QUEUE_SIZE = "VARIANT_ES_BULK_REQUEST_QUEUE_SIZE";
    public static final String VARIANT_ES_BULK_ACTION_SIZE = "VARIANT_ES_BULK_ACTION_SIZE";
    public static final String VARIANT_ES_BULK_CONCURRENT_REQUEST_AMOUNT = "VARIANT_ES_BULK_CONCURRENT_REQUEST_AMOUNT";
    public static final String VARIANT_ES_BULK_SIZE_MB = "VARIANT_ES_BULK_SIZE_MB";
    public static final String VARIANT_ES_INDEX_NUMBER_OF_SHARDS = "VARIANT_ES_INDEX_NUMBER_OF_SHARDS";

}
