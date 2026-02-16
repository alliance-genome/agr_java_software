package org.alliancegenome.indexer.variant.es.managers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.HeaderParam;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.filedownload.model.DownloadFileSet;
import org.alliancegenome.core.filedownload.model.DownloadSource;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.curation_api.model.entities.ResourceDescriptor;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.neo4j.repository.indexer.GeneIndexerRepository;
import si.mazi.rescu.ClientConfig;
import si.mazi.rescu.RestProxyFactory;
import si.mazi.rescu.serialization.jackson.JacksonObjectMapperFactory;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class SourceDocumentCreationManager extends Thread {

	private DownloadFileSet downloadSet;

	public SourceDocumentCreationManager(DownloadFileSet downloadSet) {
		this.downloadSet = downloadSet;
	}

	@Override
	public void run() {

		try {

			ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getSourceDocumentCreatorThreads());

			GeneIndexerRepository geneRepo = new GeneIndexerRepository();
			GeneDocumentCache geneCache = geneRepo.getGeneCacheCrossReferencesSynonyms();
			geneRepo.close();

			// retrieve ResourceDescriptors
			ClientConfig clientConfig = new ClientConfig();
			clientConfig.setJacksonObjectMapperFactory(new JacksonObjectMapperFactory() {
				@Override
				public ObjectMapper createObjectMapper() {
					JsonFactory factory = JsonFactory.builder()
						.enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
						.build();
					ObjectMapper mapper = new ObjectMapper(factory);
					mapper.registerModule(new JavaTimeModule());
					mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
					return mapper;
				}

				@Override
				public void configureObjectMapper(ObjectMapper mapper) {
				}
			});
			clientConfig.addDefaultParam(HeaderParam.class, "Content-Type", "application/json");
			clientConfig.setHttpConnTimeout(300000);
			clientConfig.setHttpReadTimeout(300000);

			ResourceDescriptorInterface rdApi = RestProxyFactory.createProxy(ResourceDescriptorInterface.class, ConfigHelper.getCurationApiUrl(), clientConfig);
			HashMap<String, Object> params = new HashMap<>();
			SearchResponse<ResourceDescriptor> response = rdApi.find(0, 100, params);
			List<ResourceDescriptor> resourceDescriptorList = response.getResults();

			for (DownloadSource source : downloadSet.getDownloadFileSources()) {
				if (source.getActive()) {
					SourceDocumentCreation creator = new SourceDocumentCreation(downloadSet.getDownloadPath(), source, geneCache, resourceDescriptorList);
					executor.execute(creator);
				}
			}
			log.info("SourceDocumentCreationManager shutting down executor... ");
			executor.shutdown();
			while (!executor.isTerminated()) {
				Thread.sleep(1000);
			}
			log.info("SourceDocumentCreationManager executor shut down: ");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
