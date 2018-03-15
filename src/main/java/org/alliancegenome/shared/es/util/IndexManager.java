package org.alliancegenome.shared.es.util;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.alliancegenome.shared.config.ConfigHelper;
import org.alliancegenome.shared.es.schema.settings.SiteIndexSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryResponse;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.health.ClusterIndexHealth;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;

public class IndexManager {

	private final Logger log = LogManager.getLogger(getClass());
	private PreBuiltXPackTransportClient client;
	private String newIndexName;
	private String oldIndexName;
	private String baseIndexName = "site_index";

	public IndexManager() {
		try {
			client = new PreBuiltXPackTransportClient(Settings.EMPTY);
			client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ConfigHelper.getEsHost()), ConfigHelper.getEsPort()));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void createAlias(String alias, String index) {
		log.debug("Creating Alias: " + alias + " for index: " + index);
		client.admin().indices().prepareAliases().addAlias(index, alias).get();
	}
	public void removeAlias(String alias, String index) {
		log.debug("Removing Alias: " + alias + " for index: " + index);
		client.admin().indices().prepareAliases().removeAlias(index, alias).get();
	}

	public void createIndex(String index) {
		log.debug("Creating index: " + index);

		try {
			SiteIndexSettings settings = new SiteIndexSettings(true);
			settings.buildSettings();
			client.admin().indices().create(new CreateIndexRequest(index).settings(settings.getBuilder().string(), XContentType.JSON)).get();
			//log.debug(t.toString());
		} catch (Exception e) {
			client.admin().indices().prepareRefresh(index).get();
			log.error("Indexing Failed: " + index);
			e.printStackTrace();
			System.exit(0);
		}
	}
	public void deleteIndex(String index) {
		log.info("Deleting Index: " + index);
		client.admin().indices().prepareDelete(index).get();
	}

	public void deleteIndices(List<String> indices) {
		log.info("Deleting Indices: " + indices);
		String[] array = new String[indices.size()];
		indices.toArray(array);
		client.admin().indices().prepareDelete(array).get();
	}

	public void startIndex() {
		if(ConfigHelper.hasEsIndexSuffix()) {
			newIndexName = baseIndexName + "_" + ConfigHelper.getEsIndexSuffix() + "_" + (new Date()).getTime();
		} else {
			newIndexName = baseIndexName + "_" + (new Date()).getTime();
		}

		setCurrentIndex();
		createIndex(newIndexName);
		if(oldIndexName == null) {
			createAlias(baseIndexName, newIndexName);
		}
		log.debug("Main Index Starting: ");
	}

	public void finishIndex() {
		log.debug("Main Index Finished: ");
		client.admin().indices().prepareRefresh(newIndexName).get();

		if (oldIndexName != null) {
			removeAlias(baseIndexName, oldIndexName);
		}

		if (oldIndexName != baseIndexName) {
			createAlias(baseIndexName, newIndexName);
			if (oldIndexName != null) {
				deleteIndex(oldIndexName);
			}
		}
		takeSnapShot();
		client.close();
		log.debug(baseIndexName + " Finished: ");
	}

	//	private void addMapping() {
	//		try {
	//			Mappings mappingClass = (Mappings) indexerConfig.getMappingsClazz().getDeclaredConstructor(Boolean.class).newInstance(true);
	//			mappingClass.buildMappings();
	//			log.debug("Getting Mapping for type: " + indexerConfig.getTypeName());
	//			client.admin().indices().preparePutMapping(currentIndex).setType(indexerConfig.getTypeName()).setSource(mappingClass.getBuilder().string()).get();
	//		} catch (Exception e) {
	//			e.printStackTrace();
	//		}
	//	}


	public String getCreateRepo(String repoName) {
		try {
			List<RepositoryMetaData> repositories = client.admin().cluster().prepareGetRepositories().get().repositories();

			if(repositories.size() == 0) {
				log.debug("No Repo's found - Creating Repo");
				return createRepo(repoName);
			} else {
				for(RepositoryMetaData repo: repositories) {
					if(repo.name().equals(repoName)) {
						return repo.name();
					}
				}
				return createRepo(repoName);
			}
		} catch (Exception ex){
			log.error("Exception in getRepository method: " + ex.toString());
		}

		return null;
	}

	public List<RepositoryMetaData> listRepos() {
		try {
			List<RepositoryMetaData> repositories = client.admin().cluster().prepareGetRepositories().get().repositories();
			return repositories;
		} catch (Exception ex){
			log.error("Exception in getRepository method: " + ex.toString());
		}
		return null;
	}

	public void takeSnapShot() {
		String repo = getCreateRepo(ConfigHelper.getEsIndexSuffix());

		if(repo != null) {
			List<String> indices = new ArrayList<>();
			indices.add(baseIndexName);
			log.info("Creating Snapshot: " + newIndexName + " for index: " + baseIndexName);
			createSnapShot(ConfigHelper.getEsIndexSuffix(), newIndexName, indices);
		}
	}
	
	public void createSnapShot(String repo, String snapShotName, List<String> indices) {
		try {
			log.info("Creating Snapshot: " + snapShotName + " in: " + repo + " with: " + indices);
			String[] array = new String[indices.size()];
			indices.toArray(array);
			client.admin().cluster()
			.prepareCreateSnapshot(repo, snapShotName)
			.setWaitForCompletion(true)
			.setIndices(array).get();
			log.info("Snapshot " + snapShotName + " was created for indices: " + indices);
		} catch (Exception ex){
			log.error("Exception in createSnapshot method: " + ex.toString());
		}
	}

	private String createRepo(String repoName) {

		if(ConfigHelper.getAWSAccessKey() != null && ConfigHelper.getAWSAccessKey() != null && repoName != null) {
			try {
				SiteIndexSettings settings = new SiteIndexSettings(true);
				settings.buildRepositorySettings("agr-es-backup-" + repoName, ConfigHelper.getAWSAccessKey(), ConfigHelper.getAWSSecretKey());
				log.info(repoName + " -> " + settings.getBuilder().string());
				PutRepositoryResponse putRepositoryResponse = client.admin().cluster().preparePutRepository(repoName).setType("s3").setSettings(settings.getBuilder().string(), XContentType.JSON).get();
				log.info("Repository was created: " + putRepositoryResponse.toString());
				return repoName;
			} catch(Exception ex) {
				log.error("Exception in createRepository method: " + ex.toString());
			}
		} else {
			log.info("Skipping Creation of Repo No AWS Creds or Index Suffix");
		}
		return null;
	}

	public void setCurrentIndex() {
		try {
			GetIndexResponse t = client.admin().indices().prepareGetIndex().addIndices(baseIndexName).get();
			//log.debug("Index Found: " + t.getIndices()[0]);
			oldIndexName = t.getIndices()[0];
		} catch (Exception e) {
			oldIndexName = null;
		}
		log.debug("Current Index: " + oldIndexName);

		if(oldIndexName != null) {
			ImmutableOpenMap<String, IndexMetaData> indexList = client.admin().cluster().prepareState().get().getState().getMetaData().getIndices();
			Iterator<String> keys = indexList.keysIt();
			while(keys.hasNext()) {
				String key = keys.next();
				IndexMetaData index = indexList.get(key);

				if(!index.getIndex().getName().equals(oldIndexName)) {
					if(index.getIndex().getName().startsWith(baseIndexName + "_")) {
						deleteIndex(index.getIndex().getName());
					}
				}
			}
		}
	}

	public String getNewIndexName() {
		return newIndexName;
	}

	public List<SnapshotInfo> getSnapshots(String repo) {
		GetSnapshotsResponse res  = client.admin().cluster().prepareGetSnapshots(repo).get();
		return res.getSnapshots();
	}

	public List<String> getIndexList() {
		List<String> ret = new ArrayList<>();
		ClusterHealthResponse healths = client.admin().cluster().prepareHealth().get(); 
		String clusterName = healths.getClusterName();              
		int numberOfDataNodes = healths.getNumberOfDataNodes();     
		int numberOfNodes = healths.getNumberOfNodes();             

		for (ClusterIndexHealth health : healths.getIndices().values()) { 
			String index = health.getIndex();   
			ret.add(index);
			int numberOfShards = health.getNumberOfShards();        
			int numberOfReplicas = health.getNumberOfReplicas();    
			ClusterHealthStatus status = health.getStatus();        
		}
		return ret;
	}

}
