package org.alliancegenome.es.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.site.schema.Mapping;
import org.alliancegenome.es.index.site.schema.settings.SiteIndexSettings;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
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
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.cluster.health.ClusterIndexHealth;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.snapshots.SnapshotInfo;

public class IndexManager {

    private final Logger log = LogManager.getLogger(getClass());
    private RestHighLevelClient client = null;
    private String newIndexName;
    private String baseIndexName = "site_index";
    private String tempIndexName = "site_index_temp";

    public IndexManager() {
        initClient();
    }

    public void initClient() {
        if(ConfigHelper.getEsHost().contains(",")) {
            String[] hostnames = ConfigHelper.getEsHost().split(",");
            List<HttpHost> hosts = Arrays.stream(hostnames).map(host -> new HttpHost(host, ConfigHelper.getEsPort())).collect(Collectors.toList());
            client = new RestHighLevelClient(
                RestClient.builder((HttpHost[])hosts.toArray())
                .setRequestConfigCallback(
                        new RequestConfigCallback() {
                            @Override
                            public Builder customizeRequestConfig(Builder requestConfigBuilder) {
                                return requestConfigBuilder
                                        .setConnectTimeout(5000)
                                        .setSocketTimeout(1800000)
                                        .setConnectionRequestTimeout(1800000)
                                        ;
                            }
                        }
                    )
                );
        } else {
            client = new RestHighLevelClient(RestClient.builder(new HttpHost(ConfigHelper.getEsHost(),ConfigHelper.getEsPort())));
        }
    }

    public void createAlias(String alias, String index) {
        log.debug("Creating Alias: " + alias + " for index: " + index);


        String realIndexName = getIndexNameForAlias(index);
        if (realIndexName != null ) { index = realIndexName; }

        IndicesAliasesRequest request = new IndicesAliasesRequest();
        IndicesAliasesRequest.AliasActions aliasAction =
                new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                .index(index)
                .alias(alias);
        request.addAliasAction(aliasAction);

        try {
            client.indices().updateAliases(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void removeAlias(String alias, String index) {
        log.debug("Removing Alias: " + alias + " for index: " + index);

        IndicesAliasesRequest request = new IndicesAliasesRequest();
        IndicesAliasesRequest.AliasActions removeAction =
                new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
                .index(index)
                .alias(alias);
        request.addAliasAction(removeAction);

        try {
            client.indices().updateAliases(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createIndex(String index, boolean addMappings) {
        log.debug("Creating index: " + index);
        System.out.println("creating index");
        try {
            SiteIndexSettings settings = new SiteIndexSettings(true);
            Mapping mapping = new Mapping(true);
            settings.buildSettings();
            CreateIndexRequest request = new CreateIndexRequest(index);
            request.settings(settings.getBuilder());

            if(addMappings) {
                mapping.buildMapping();
                request.mapping(mapping.getBuilder());
            }
            client.indices().create(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            RefreshRequest refreshRequest = new RefreshRequest(index);
            try {
                client.indices().refresh(refreshRequest, RequestOptions.DEFAULT);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            log.error("Indexing Failed: " + index);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public List<String> getAliasesForIndex(String index) {

        List<String> aliases = new ArrayList<>();

        GetAliasesResponse response = null;
        try {
            GetAliasesRequest request = new GetAliasesRequest();
            response = client.indices().getAlias(request, RequestOptions.DEFAULT);

        } catch (IOException e) {
            e.printStackTrace();
        }

        Set<AliasMetaData> aliasMetaDataSet = response.getAliases().get(index);

        if (CollectionUtils.isNotEmpty(aliasMetaDataSet)) {
            aliases.addAll(aliasMetaDataSet.stream().map(AliasMetaData::alias).collect(Collectors.toList()));
        }

        return aliases;
    }

    public String getIndexNameForAlias(String alias) {

        GetAliasesResponse response = null;
        try {
            GetAliasesRequest request = new GetAliasesRequest();
            response = client.indices().getAlias(request, RequestOptions.DEFAULT);

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response.getAliases() == null || response.getAliases().size() == 0) {
            return null;
        }

        for (String index : response.getAliases().keySet()) {
            for (AliasMetaData aliasMetaData : response.getAliases().get(index)) {
                if (StringUtils.equals(alias, aliasMetaData.getAlias())) {
                    return index;
                }
            }
        }
        return null;
    }

    public void deleteIndex(String index) {
        log.info("Deleting Index: " + index);
        DeleteIndexRequest request = new DeleteIndexRequest(index);
        try {
            client.indices().delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteIndices(List<String> indices) {
        log.info("Deleting Indices: " + indices);
        for (String index : indices) {
            deleteIndex(index);
        }
    }

    public String startSiteIndex() {
        if(ConfigHelper.hasEsIndexSuffix()) {
            newIndexName = baseIndexName + "_" + ConfigHelper.getEsIndexSuffix() + "_" + (new Date()).getTime();
        } else {
            newIndexName = baseIndexName + "_" + (new Date()).getTime();
        }

        createIndex(newIndexName, true);
        for (String alias : getAliasesForIndex(tempIndexName)) {
            removeAlias(alias, tempIndexName);
        }

        String indexName = getIndexNameForAlias(tempIndexName);
        if (StringUtils.isNotEmpty(indexName)) {
            removeAlias(tempIndexName, indexName);
        }

        createAlias(tempIndexName, newIndexName);

        log.debug("Main Index Starting: ");
        return newIndexName;
    }


    public void finishIndex() {
        log.debug("Main Index Finished: ");
        RefreshRequest request = new RefreshRequest(newIndexName);
        try {
            client.indices().refresh(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        takeSnapShot();

        List<String> baseIndexAliases = getAliasesForIndex(baseIndexName);
        if (baseIndexAliases != null && baseIndexAliases.contains(baseIndexName)) {
            removeAlias(baseIndexName, baseIndexName);
        }

        createAlias(baseIndexName, tempIndexName);
        removeOldIndexes();
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.debug(baseIndexName + " Finished: ");
    }

    private void removeOldIndexes() {
        List<String> indexes = getIndexList();
        for(String indexName: indexes) {
            if(indexName.contains(baseIndexName)) {
                List<String> aliases = getAliasesForIndex(indexName);

                if(CollectionUtils.isNotEmpty(aliases)
                        && !aliases.contains(baseIndexName)
                        && !aliases.contains(tempIndexName)) {
                    log.debug("Removing Old Index: " + indexName);
                    deleteIndex(indexName);
                }
            }
        }
    }

    public String getCreateRepo(String repoName) {
        try {

            GetRepositoriesRequest request = new GetRepositoriesRequest();
            GetRepositoriesResponse response = client.snapshot().getRepository(request, RequestOptions.DEFAULT);
            List<RepositoryMetaData> repositories = response.repositories();

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
            GetRepositoriesRequest request = new GetRepositoriesRequest();
            GetRepositoriesResponse response = client.snapshot().getRepository(request, RequestOptions.DEFAULT);
            List<RepositoryMetaData> repositories = response.repositories();

            return repositories;
        } catch (Exception ex) {
            log.error("Exception in getRepository method: " + ex.toString());
        }
        return null;
    }

    public void takeSnapShot() {
        String repo = getCreateRepo(ConfigHelper.getEsIndexSuffix());

        if(repo != null) {
            List<String> indices = new ArrayList<>();
            indices.add(newIndexName);
            log.info("Creating Snapshot: " + newIndexName + " for index: " + newIndexName);
            createSnapShot(ConfigHelper.getEsIndexSuffix(), newIndexName, indices);
        }
    }

    public void deleteSnapShot(String repo, String snapShotName) {
        try {
            log.info("Deleting Snapshot: " + snapShotName + " in: " + repo);
            DeleteSnapshotRequest request = new DeleteSnapshotRequest(repo);
            request.snapshot(snapShotName);
            client.snapshot().delete(request, RequestOptions.DEFAULT);
        } catch (Exception ex) {
            log.error("Exception in restoreSnapShot method: " + ex.toString());
        }
    }

    public void restoreSnapShot(String repo, String snapShotName, List<String> indices) {
        try {
            log.info("Restoring Snapshot: " + snapShotName + " in: " + repo + " with: " + indices);
            String[] array = new String[indices.size()];
            indices.toArray(array);
            checkRepo(repo);

            RestoreSnapshotRequest request = new RestoreSnapshotRequest(repo, snapShotName);
            request.indices(indices);
            client.snapshot().restore(request, RequestOptions.DEFAULT);

        } catch (Exception ex) {
            log.error("Exception in restoreSnapShot method: " + ex.toString());
        }
    }

    public void createSnapShot(String repo, String snapShotName, List<String> indices) {
        try {
            log.info("Creating Snapshot: " + snapShotName + " in: " + repo + " with: " + indices);

            CreateSnapshotRequest request = new CreateSnapshotRequest();
            request.repository(repo);
            request.snapshot(snapShotName);
            request.indices(indices);
            request.waitForCompletion(true);

            client.snapshot().create(request, RequestOptions.DEFAULT);

            log.info("Snapshot " + snapShotName + " was created for indices: " + indices);
        } catch (Exception ex){
            log.error("Exception in createSnapshot method: " + ex.toString());
        }
    }

    private String createRepo(String repoName) {

        if(repoName != null) {
            try {

                Settings settings = Settings.builder().put("bucket", "agr-es-backup-" + repoName).build();

                PutRepositoryRequest request = new PutRepositoryRequest();
                request.settings(settings);
                request.name(repoName);
                request.type("s3");
                request.verify(true);
                request.timeout(new TimeValue(30, TimeUnit.MINUTES));

                log.info(repoName + " -> " + settings.toString());

                AcknowledgedResponse response = client.snapshot().createRepository(request, RequestOptions.DEFAULT);

                log.info("Repository was created: " + response.toString());
                return repoName;
            } catch(Exception ex) {
                log.error("Exception in createRepository method: " + ex.toString());
            }
        } else {
            log.info("Skipping Creation of Repo No AWS Creds or Index Suffix");
        }
        return null;
    }

    public List<SnapshotInfo> getSnapshots(String repo) {
        checkRepo(repo);

        GetSnapshotsRequest request = new GetSnapshotsRequest();
        request.repository(repo);

        GetSnapshotsResponse response = null;

        try {
            response = client.snapshot().get(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response == null) { return null; }

        return response.getSnapshots();
    }

    public List<String> getIndexList() {

        ClusterHealthRequest request = new ClusterHealthRequest();

        Map<String, ClusterIndexHealth> healths = null;

        try {
            ClusterHealthResponse response = client.cluster().health(request, RequestOptions.DEFAULT);
            healths = response.getIndices();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (healths == null) { return null; }

        return new ArrayList<String>(healths.keySet());
    }

    private void checkRepo(String repo) {
        boolean found = false;
        List<RepositoryMetaData> meta = listRepos();
        for(RepositoryMetaData data: meta) {
            if(data.name().equals(repo)) {
                found = true;
                System.out.println("Repo Found Name: " + data.name() + " Type: " + data.type());
                break;
            }
        }

        if(!found) {
            System.out.println("Repo Not Found: " + repo);
            getCreateRepo(repo);
        }
    }

    public String getBaseIndexName() { return baseIndexName; }
}
