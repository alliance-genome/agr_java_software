package org.alliancegenome.core.es.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.es.schema.settings.SiteIndexSettings;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.repositories.delete.DeleteRepositoryRequest;
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesRequest;
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesResponse;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.cluster.metadata.RepositoryMetadata;
import org.elasticsearch.common.Strings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.snapshots.RestoreInfo;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.elasticsearch.snapshots.SnapshotState;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IndexManager {

	private String basePath = "";
	private String timeStamp = "";

	RestHighLevelClient closableSearchClient;

	public IndexManager() { }
	
	public IndexManager(String basePath) {
		if (basePath != null) {
			this.basePath = basePath;
		}
		closableSearchClient = EsClientFactory.getMustCloseSearchClient();
	}

	public void setBasePath(String basePath) {
		if (basePath != null) {
			this.basePath = basePath;
		} else {
			this.basePath = "";
		}
	}

	public String getBasePath() {
		return basePath;
	}

	public void createAlias(String alias, String index) { // ES Util
		log.info("Creating Alias: " + alias + " for index: " + index);

		IndicesAliasesRequest request = new IndicesAliasesRequest();
		IndicesAliasesRequest.AliasActions aliasAction = new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD).index(index).alias(alias);
		request.addAliasAction(aliasAction);

		try {
			closableSearchClient.indices().updateAliases(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
		}
	}

	public void removeAlias(String alias, String index) { // ES Util

		log.info("Removing Alias: " + alias + " for index: " + index);

		IndicesAliasesRequest request = new IndicesAliasesRequest();
		IndicesAliasesRequest.AliasActions removeAction = new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE).index(index).alias(alias);
		request.addAliasAction(removeAction);

		try {
			closableSearchClient.indices().updateAliases(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
		}

	}

	private void createIndex(IndexName indexName, String newIndexName) {
		log.info("Creating index: " + newIndexName);
		try {
			CreateIndexRequest createIndexRequest = new CreateIndexRequest(newIndexName);
			if (indexName.getSettings() != null) {
				createIndexRequest.settings(indexName.getSettings().getBuilder());
			}
			if (indexName.getMapping() != null) {
				createIndexRequest.mapping(indexName.getMapping().getBuilder());
			}
			closableSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
		} catch (Exception e) {
			log.error("Index creation failed: " + newIndexName, e);
			ExceptionCatcher.report(e);
			System.exit(-1);
		}
	}

	public List<String> getAliasesForIndex(String index) { // ES Util

		List<String> aliases = new ArrayList<>();

		GetAliasesResponse response = null;
		try {
			GetAliasesRequest request = new GetAliasesRequest();
			response = closableSearchClient.indices().getAlias(request, RequestOptions.DEFAULT);

		} catch (IOException e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
		}

		Set<AliasMetadata> aliasMetaDataSet = response.getAliases().get(index);

		if (CollectionUtils.isNotEmpty(aliasMetaDataSet)) {
			aliases.addAll(aliasMetaDataSet.stream().map(AliasMetadata::alias).collect(Collectors.toList()));
		}

		return aliases;
	}

	private List<String> getIndexNamesFromAlias(String alias) {

		GetAliasesResponse response = null;
		try {
			GetAliasesRequest request = new GetAliasesRequest();
			response = closableSearchClient.indices().getAlias(request, RequestOptions.DEFAULT);

		} catch (IOException e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
		}

		if (response.getAliases() == null || response.getAliases().size() == 0) {
			return new ArrayList<String>();
		}

		List<String> ret = new ArrayList<String>();
		for (String index : response.getAliases().keySet()) {
			for (AliasMetadata aliasMetaData : response.getAliases().get(index)) {
				if (StringUtils.equals(alias, aliasMetaData.getAlias())) {
					ret.add(index);
				}
			}
		}
		return ret;
	}

	private void deleteIndex(String index) {
		log.info("Deleting Index: " + index);
		DeleteIndexRequest request = new DeleteIndexRequest(index);
		try {
			closableSearchClient.indices().delete(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
		}
	}

	public void deleteIndices(List<String> indices) { // ES Util
		log.info("Deleting Indices: " + indices);
		for (String index : indices) {
			deleteIndex(index);
		}
	}

	public void deleteRepo(String repoName) { // ES Util
		try {
			DeleteRepositoryRequest request = new DeleteRepositoryRequest();
			request.name(repoName);
			AcknowledgedResponse res = closableSearchClient.snapshot().deleteRepository(request, RequestOptions.DEFAULT);
			if (res.isAcknowledged()) {
				log.info("Deleted Repo: " + repoName);
			} else {
				log.info("Deleted Repo: " + repoName + " failed");
			}
		} catch (IOException e) {
			ExceptionCatcher.report(e);
			log.error("Exception deleting getRepository: " + e.toString());
		}
	}

	public String getCreateRepo(String repoName) { // ES Util
		try {
			GetRepositoriesRequest request = new GetRepositoriesRequest();
			GetRepositoriesResponse response = closableSearchClient.snapshot().getRepository(request, RequestOptions.DEFAULT);
			List<RepositoryMetadata> repositories = response.repositories();

			if (repositories.size() == 0) {
				log.info("No Repo's found - Creating Repo");
				return createRepo(repoName);
			} else {
				for (RepositoryMetadata repo : repositories) {
					if (repo.name().equals(repoName)) {
						return repo.name();
					}
				}
				return createRepo(repoName);
			}
		} catch (Exception ex) {
			ExceptionCatcher.report(ex);
			log.error("Exception in getRepository method: " + ex.toString());
		}

		return null;
	}

	public List<RepositoryMetadata> listRepos() { // ES Util
		try {
			GetRepositoriesRequest request = new GetRepositoriesRequest();
			GetRepositoriesResponse response = closableSearchClient.snapshot().getRepository(request, RequestOptions.DEFAULT);
			List<RepositoryMetadata> repositories = response.repositories();

			return repositories;
		} catch (Exception ex) {
			ex.printStackTrace();
			ExceptionCatcher.report(ex);
			log.error("Exception in getRepository method: " + ex.toString());
		}
		return null;
	}

	private void takeSnapShot(List<IndexName> indexes) {
		String repo = getCreateRepo(ConfigHelper.getEsIndexSuffix());

		if (repo != null) {
			List<String> indices = new ArrayList<>();
			for (IndexName indexName: indexes) {
				String index = indexName.getIndexName();
				String newIndexName = index + "_" + timeStamp;
				indices.add(newIndexName);
			}
			String snapShotName = basePath + "_snapshot_" + timeStamp;
			log.info("Creating Snapshot: " + snapShotName + " for indices: " + indices);
			createSnapShot(ConfigHelper.getEsIndexSuffix(), snapShotName, indices);
		}
	}

	public void deleteSnapShot(String repo, String snapShotName) { // ES Util
		String[] array;
		if (snapShotName.contains(",")) {
			array = snapShotName.split(",");
		} else {
			array = new String[1];
			array[0] = snapShotName;
		}

		try {
			for (int i = 0; i < array.length; i++) {
				log.info("Deleting Snapshot: " + array[i] + " in: " + repo);
				DeleteSnapshotRequest request = new DeleteSnapshotRequest(repo, array[i]);
				closableSearchClient.snapshot().delete(request, RequestOptions.DEFAULT);
			}
		} catch (Exception ex) {
			ExceptionCatcher.report(ex);
			log.error("Exception in deleteSnapShot method: " + ex.toString());
		}
	}

	private boolean restoreSnapShot(String repo, String snapShotName, List<String> indices) { // ES Util
		try {
			log.info("Restoring Snapshot: " + snapShotName + " in: " + repo + " with: " + indices);
			String[] array = new String[indices.size()];
			indices.toArray(array);
			checkRepo(repo);

			RestoreSnapshotRequest request = new RestoreSnapshotRequest(repo, snapShotName);
			request.indices(indices);
			request.includeAliases(false);
			request.waitForCompletion(true);
			RestoreSnapshotResponse response = closableSearchClient.snapshot().restore(request, RequestOptions.DEFAULT);
			RestoreInfo restoreInfo = response.getRestoreInfo();
			if (restoreInfo != null && restoreInfo.failedShards() == 0) {
				return true;
			}
			log.error("Restore of " + snapShotName + " did not complete: " + restoreInfo);
		} catch (Exception ex) {
			ExceptionCatcher.report(ex);
			log.error("Exception in restoreSnapShot method: " + ex.toString());
		}
		return false;
	}

	public void createSnapShot(String repo, String snapShotName, String index) { // ES Util
		List<String> indices = new ArrayList<String>();
		indices.add(index);
		createSnapShot(repo, snapShotName, indices);
	}

	public void createSnapShot(String repo, String snapShotName, List<String> indices) { // ES Util
		try {
			log.info("Creating Snapshot: " + snapShotName + " in: " + repo + " with: " + indices);

			CreateSnapshotRequest request = new CreateSnapshotRequest();
			request.repository(repo);
			request.snapshot(snapShotName);
			request.indices(indices);
			request.waitForCompletion(true);

			closableSearchClient.snapshot().create(request, RequestOptions.DEFAULT);

			log.info("Snapshot " + snapShotName + " was created for indices: " + indices);
		} catch (Exception ex) {
			ExceptionCatcher.report(ex);
			ex.printStackTrace();
			System.exit(-1);
		}
	}

	private String createRepo(String repoName) {

		if (repoName != null && repoName.length() > 0) {
			try {

				SiteIndexSettings settings = new SiteIndexSettings(true, 1);
				String fullBasePath = "";
				if (basePath != null && !basePath.isEmpty()) {
					fullBasePath = ConfigHelper.getEsIndexSuffix() + "/" + basePath;
				}
				settings.buildRepositorySettings("agr-es-backup", fullBasePath);

				PutRepositoryRequest request = new PutRepositoryRequest();
				request.settings(Strings.toString(settings.getBuilder()), settings.getBuilder().contentType());
				request.name(repoName);
				request.type("s3");
				request.verify(true);
				request.timeout(new TimeValue(30, TimeUnit.MINUTES));

				log.info(repoName + " -> s3://agr-es-backup/" + fullBasePath);

				AcknowledgedResponse response = closableSearchClient.snapshot().createRepository(request, RequestOptions.DEFAULT);

				log.info("Repository was created: " + response.toString());
				return repoName;
			} catch (Exception ex) {
				ExceptionCatcher.report(ex);
				log.error("Exception in createRepository method: " + ex.toString());
			}
		} else {
			log.info("Skipping Creation of Repo No AWS Creds or Index Suffix");
		}
		return null;
	}

	private List<SnapshotInfo> getSnapshots(String repo) { // ES Util
		checkRepo(repo);

		GetSnapshotsRequest request = new GetSnapshotsRequest();
		request.repository(repo);

		GetSnapshotsResponse response = null;

		try {
			response = closableSearchClient.snapshot().get(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
		}

		if (response == null) {
			return null;
		}

		return response.getSnapshots();
	}

	public List<String> getIndexList() { // ES Util
		try {
			GetIndexRequest request = new GetIndexRequest("*");
			GetIndexResponse response = closableSearchClient.indices().get(request, RequestOptions.DEFAULT);
			String[] indices = response.getIndices();
			return new ArrayList<String>(Arrays.asList(indices));

		} catch (Exception e) {
			ExceptionCatcher.report(e);
			log.error("No Indexes found: " + e.getLocalizedMessage());
		}

		return new ArrayList<String>();
	}

	private void checkRepo(String repo) {
		boolean found = false;
		List<RepositoryMetadata> meta = listRepos();
		for (RepositoryMetadata data : meta) {
			if (data.name().equals(repo)) {
				found = true;
				log.info("Repo Found Name: " + data.name() + " Type: " + data.type());
				break;
			}
		}

		if (!found) {
			log.info("Repo Not Found: " + repo);
			getCreateRepo(repo);
		}
	}

	public void startIndexes(List<IndexName> indexes) {

		timeStamp = (new Date()).getTime() + "";
		
		for (IndexName name: indexes) {
			String indexName = name.getIndexName();
			String newIndexName = indexName + "_" + timeStamp;
			String tempIndexName = newIndexName + "_temp";
			createIndex(name, newIndexName);
			createAlias(indexName, newIndexName);
			createAlias(tempIndexName, newIndexName);
		}
		log.info("Main Indexer Starting: ");
	}

	public void finishIndex(List<IndexName> indexes) {
		log.info("Main Indexer Finished: ");
		
		for (IndexName name: indexes) {
			String indexName = name.getIndexName();
			String newIndexName = indexName + "_" + timeStamp;
			
			RefreshRequest request = new RefreshRequest(newIndexName);
			try {
				closableSearchClient.indices().refresh(request, RequestOptions.DEFAULT);
			} catch (IOException e) {
				ExceptionCatcher.report(e);
				e.printStackTrace();
			}
		}

		takeSnapShot(indexes);

		try {
			closableSearchClient.close();
		} catch (IOException e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
		}

		log.info("Finished Indexing: ");
	}

	public void closeClient() throws IOException { // ES Util
		closableSearchClient.close();
	}

	public void resetClient() throws IOException { // ES Util
		if (closableSearchClient != null) {
			closableSearchClient.close();
		}
		closableSearchClient = EsClientFactory.getMustCloseSearchClient();
	}

	public void listRepo(String repo) { // ES Util
		List<SnapshotInfo> list = getSnapshots(repo);
		for (SnapshotInfo info : list) {
			Date end = new Date(info.endTime());
			String delim = "";
			String print = "";
			for (String index : info.indices()) {
				print += delim + index;
				delim = ",";
			}
			log.info(info.snapshotId() + "[" + print + "] " + end);
		}
	}

	public void cleanSnapShots(String repo, String snapShotName) {
		List<SnapshotInfo> list = getSnapshots(repo);
		TreeMap<Date, SnapshotInfo> map = new TreeMap<>();
		for (SnapshotInfo info : list) {
			String[] array = info.snapshotId().getName().split("_");
			Date d = new Date(Long.parseLong(array[array.length - 1]));
			if (info.snapshotId().getName().contains(snapShotName)) {
				map.put(d, info);
			}
		}
		if (map.size() > 0) {
			map.remove(map.firstKey()); // Keep the oldest snapshot, as this is presumably what is on production
			map.remove(map.lastKey()); // Keep the latest snapshot
			for (Date key : map.keySet()) {
				deleteSnapShot(repo, map.get(key).snapshotId().getName());
			}
		}
	}

	public void restoreSnapShot(String repo) { // ES Util
		String prefix = basePath + "_snapshot_";
		String groupAlias = basePath + "_snapshot";

		List<SnapshotInfo> list = getSnapshots(repo);
		if (list == null) {
			log.error("Unable to list snapshots in: " + repo);
			return;
		}
		TreeMap<Long, SnapshotInfo> map = new TreeMap<>();
		for (SnapshotInfo info : list) {
			String name = info.snapshotId().getName();
			if (info.state() == SnapshotState.SUCCESS && name.startsWith(prefix)) {
				try {
					map.put(Long.parseLong(name.substring(prefix.length())), info);
				} catch (NumberFormatException e) {
					log.warn("Can't parse timestamp from snapshot: " + name + " skipping");
				}
			}
		}
		if (map.isEmpty()) {
			log.error("No successful " + prefix + "* snapshots found in: " + repo);
			return;
		}

		SnapshotInfo latest = map.lastEntry().getValue();
		String snapShotName = latest.snapshotId().getName();
		List<String> newIndices = latest.indices();
		log.info("Latest Snapshot: " + snapShotName + " with: " + newIndices);

		// The group alias marks the indices from the last restore that went live
		List<String> oldIndices = getIndexNamesFromAlias(groupAlias);
		if (new HashSet<>(oldIndices).equals(new HashSet<>(newIndices))) {
			log.info(snapShotName + " is already live under: " + groupAlias);
			return;
		}

		List<String> existing = getIndexList();
		List<String> missing = newIndices.stream().filter(index -> !existing.contains(index)).collect(Collectors.toList());
		if (!missing.isEmpty() && !restoreSnapShot(repo, snapShotName, missing)) {
			log.error("Restore failed, leaving aliases unchanged");
			return;
		}

		try {
			ClusterHealthRequest healthRequest = new ClusterHealthRequest(newIndices.toArray(new String[0])).waitForGreenStatus().timeout(TimeValue.timeValueHours(1));
			ClusterHealthResponse health = closableSearchClient.cluster().health(healthRequest, RequestOptions.DEFAULT);
			if (health.isTimedOut() || health.getStatus() != ClusterHealthStatus.GREEN) {
				log.error("Restored indices are " + health.getStatus() + ", leaving aliases unchanged: " + newIndices);
				return;
			}
		} catch (Exception e) {
			ExceptionCatcher.report(e);
			log.error("Health check failed, leaving aliases unchanged: " + e.getMessage());
			return;
		}

		// One request so the serving aliases, the group alias and the old indices all switch at once
		IndicesAliasesRequest request = new IndicesAliasesRequest();
		for (String index : newIndices) {
			String alias = index.substring(0, index.lastIndexOf('_'));
			log.info("Adding Aliases: " + alias + ", " + groupAlias + " to index: " + index);
			request.addAliasAction(IndicesAliasesRequest.AliasActions.add().index(index).aliases(alias, groupAlias));
		}
		for (String index : oldIndices) {
			if (!newIndices.contains(index)) {
				log.info("Removing Index: " + index);
				request.addAliasAction(IndicesAliasesRequest.AliasActions.removeIndex().index(index));
			}
		}

		try {
			closableSearchClient.indices().updateAliases(request, RequestOptions.DEFAULT);
			log.info(snapShotName + " is live");
		} catch (Exception e) {
			ExceptionCatcher.report(e);
			log.error("Alias switch failed, nothing was changed: " + e.getMessage());
		}
	}

}
