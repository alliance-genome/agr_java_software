package org.alliancegenome.core.es.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.es.schema.Mapping;
import org.alliancegenome.core.es.schema.Settings;
import org.alliancegenome.core.es.schema.settings.SiteIndexSettings;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.cluster.repositories.delete.DeleteRepositoryRequest;
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesRequest;
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesResponse;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.cluster.metadata.RepositoryMetadata;
import org.elasticsearch.common.Strings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.snapshots.SnapshotInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IndexManager {

	private String newIndexName;
	private String tempIndexName;
	private String baseIndexName = ConfigHelper.getEsIndex();
	private String basePath = "";

	private Settings settings;
	private Mapping mapping;

	RestHighLevelClient closableSearchClient;

	public IndexManager(Settings settings, Mapping mapping, String basePath) {
		this.settings = settings;
		this.mapping = mapping;
		if (basePath != null) {
			this.basePath = basePath;
		}
		closableSearchClient = EsClientFactory.getMustCloseSearchClient();
	}

	public IndexManager(Settings settings, Mapping mapping) {
		this(settings, mapping, "");
	}

	public IndexManager(Settings settings, String basePath) {
		this(settings, null, basePath);
	}

	public IndexManager(String basePath) {
		this(new SiteIndexSettings(true, ConfigHelper.getEsShardCount()), new Mapping(true), basePath);
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

	private void createIndex(String index) {
		log.info("Creating index: " + index);
		try {
			CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
			if (settings != null) {
				settings.buildSettings();
				createIndexRequest.settings(settings.getBuilder());
			}

			if (mapping != null) {
				mapping.buildMapping();
				createIndexRequest.mapping(mapping.getBuilder());
			}
			closableSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
		} catch (Exception e) {
			log.error("Index creation failed: " + index, e);
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

	private void takeSnapShot() {
		String repo = getCreateRepo(ConfigHelper.getEsIndexSuffix());

		if (repo != null) {
			List<String> indices = new ArrayList<>();
			indices.add(newIndexName);
			log.info("Creating Snapshot: " + newIndexName + " for index: " + newIndexName);
			createSnapShot(ConfigHelper.getEsIndexSuffix(), newIndexName, indices);
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

	private void restoreSnapShot(String repo, String snapShotName, List<String> indices) { // ES Util
		try {
			log.info("Restoring Snapshot: " + snapShotName + " in: " + repo + " with: " + indices);
			String[] array = new String[indices.size()];
			indices.toArray(array);
			checkRepo(repo);

			RestoreSnapshotRequest request = new RestoreSnapshotRequest(repo, snapShotName);
			request.indices(indices);
			// request.includeAliases(false); TODO investigate this use then we can always
			// create indexes with aliases
			request.waitForCompletion(true);
			closableSearchClient.snapshot().restore(request, RequestOptions.DEFAULT);

		} catch (Exception ex) {
			ExceptionCatcher.report(ex);
			log.error("Exception in restoreSnapShot method: " + ex.toString());
		}
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

	public String startSiteIndex() {

		newIndexName = "";

		if (ConfigHelper.hasEsIndexPrefix()) {
			newIndexName += ConfigHelper.getEsIndexPrefix() + "_";
		}

		newIndexName += baseIndexName;

		if (ConfigHelper.hasEsIndexSuffix()) {
			newIndexName += "_" + ConfigHelper.getEsIndexSuffix();
		}

		tempIndexName = newIndexName + "_temp";
		newIndexName += "_" + (new Date()).getTime();

		createIndex(newIndexName);
		createAlias(tempIndexName, newIndexName);

		log.info("Main Index Starting: ");
		return newIndexName;
	}

	public void finishIndex() {
		log.info("Main Index Finished: ");
		RefreshRequest request = new RefreshRequest(newIndexName);
		try {
			closableSearchClient.indices().refresh(request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
		}

		takeSnapShot();

		try {
			closableSearchClient.close();
		} catch (IOException e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
		}

		log.info(baseIndexName + " Finished: ");
	}

	private void forceMerge() {
		log.info("Force merging index: " + newIndexName);
		try {
			UpdateSettingsRequest settingsRequest = new UpdateSettingsRequest(newIndexName);
			settingsRequest.settings(Map.of("index.merge.scheduler.max_thread_count", Runtime.getRuntime().availableProcessors()));
			closableSearchClient.indices().putSettings(settingsRequest, RequestOptions.DEFAULT);
			log.info("Increased merge threads for force merge");
		} catch (IOException e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
		}
		ForceMergeRequest request = new ForceMergeRequest(newIndexName);
		request.maxNumSegments(1);
		request.flush(true);
		try {
			closableSearchClient.indices().forcemerge(request, RequestOptions.DEFAULT);
			log.info("Force merge complete: " + newIndexName);
		} catch (IOException e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
		}
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

	public void restoreSnapShot(String repo, String index) {
		List<String> indexes = getIndexList();
		List<SnapshotInfo> list = getSnapshots(repo);
		TreeMap<Date, SnapshotInfo> map = new TreeMap<>();
		for (SnapshotInfo info : list) {
			String[] array = info.snapshotId().getName().split("_");
			try {
				Date d = new Date(Long.parseLong(array[array.length - 1]));
				if (info.snapshotId().getName().contains(index)) {
					map.put(d, info);
				}
			} catch (NumberFormatException e) {
				ExceptionCatcher.report(e);
				log.warn("Can't parse date from index: " + info.snapshotId().getName() + " skipping");
			}
		}
		log.info("First Snapshot: " + map.firstKey());
		log.info("Lastest Snapshot: " + map.lastKey());
		SnapshotInfo info = map.get(map.lastKey());

		if (indexes.contains(info.snapshotId().getName())) {
			log.info("Index already exists: " + info.snapshotId().getName() + " not restoring");
		} else {
			log.info("Need to restore index: " + info.snapshotId().getName());
			String snapshotName = info.snapshotId().getName();

			List<String> indexList = new ArrayList<String>();
			indexList.add(snapshotName);
			restoreSnapShot(repo, snapshotName, new ArrayList<String>(indexList));

			log.info("Restore: " + snapshotName + " is complete");
			log.info("Switching Aliases: ");
			if (indexes.size() > 0) {
				List<String> siteIndexList = getIndexNamesFromAlias("site_index");
				for (String localIndex : siteIndexList) {
					if (localIndex.contains(index)) {
						removeAlias("site_index", localIndex);
						deleteIndex(localIndex);
						break;
					}
				}
			}
			createAlias("site_index", snapshotName);
			log.info("Index restore complete");
		}

	}

}
