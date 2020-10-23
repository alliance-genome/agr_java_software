package org.alliancegenome.es.util;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.site.schema.*;
import org.alliancegenome.es.index.site.schema.settings.SiteIndexSettings;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.*;
import org.elasticsearch.action.admin.cluster.repositories.get.*;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.*;
import org.elasticsearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.cluster.metadata.*;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.snapshots.SnapshotInfo;

public class IndexManager {

    private final Logger log = LogManager.getLogger(getClass());
    private String newIndexName;
    private String baseIndexName = ConfigHelper.getEsIndex();
    private String tempIndexName = baseIndexName + "_temp";
    
    private Settings settings;
    private Mapping mapping;
    
    public IndexManager(Settings settings, Mapping mapping) {
        this.settings = settings;
        this.mapping = mapping;
    }
    
    public IndexManager(Settings settings) {
        this.settings = settings;
        this.mapping = null;
    }
    
    public IndexManager() {
        settings = new SiteIndexSettings(true);
        mapping = new Mapping(true);
    }
    
    public void resetClient() {
        EsClientFactory.createNewClient();
    }
    
    private RestHighLevelClient getClient() {
        return EsClientFactory.getDefaultEsClient();
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
            getClient().indices().updateAliases(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void removeAlias(String alias) {
        
        String index_name = getIndexNameForAlias(alias);

        log.debug("Removing Alias: " + alias + " for index: " + index_name);

        IndicesAliasesRequest request = new IndicesAliasesRequest();
        IndicesAliasesRequest.AliasActions removeAction =
                new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
                .index(index_name)
                .alias(alias);
        request.addAliasAction(removeAction);

        try {
            getClient().indices().updateAliases(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createIndex(String index) {
        log.info("Creating index: " + index);
        try {
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
            if(settings != null) {
                settings.buildSettings();
                createIndexRequest.settings(settings.getBuilder());
            }

            if(mapping != null) {
                mapping.buildMapping();
                createIndexRequest.mapping(mapping.getBuilder());
            }
            getClient().indices().create(createIndexRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            RefreshRequest refreshRequest = new RefreshRequest(index);
            try {
                getClient().indices().refresh(refreshRequest, RequestOptions.DEFAULT);
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
            response = getClient().indices().getAlias(request, RequestOptions.DEFAULT);

        } catch (IOException e) {
            e.printStackTrace();
        }

        Set<AliasMetadata> aliasMetaDataSet = response.getAliases().get(index);

        if (CollectionUtils.isNotEmpty(aliasMetaDataSet)) {
            aliases.addAll(aliasMetaDataSet.stream().map(AliasMetadata::alias).collect(Collectors.toList()));
        }

        return aliases;
    }

    public String getIndexNameForAlias(String alias) {

        GetAliasesResponse response = null;
        try {
            GetAliasesRequest request = new GetAliasesRequest();
            response = getClient().indices().getAlias(request, RequestOptions.DEFAULT);

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response.getAliases() == null || response.getAliases().size() == 0) {
            return null;
        }

        for (String index : response.getAliases().keySet()) {
            for (AliasMetadata aliasMetaData : response.getAliases().get(index)) {
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
            getClient().indices().delete(request, RequestOptions.DEFAULT);
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

        createIndex(newIndexName);
        createAlias(tempIndexName, newIndexName);

        log.debug("Main Index Starting: ");
        return newIndexName;
    }


    public void finishIndex() {
        log.debug("Main Index Finished: ");
        RefreshRequest request = new RefreshRequest(newIndexName);
        try {
            getClient().indices().refresh(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        takeSnapShot();

        EsClientFactory.closeClient();
        log.debug(baseIndexName + " Finished: ");
    }

    public String getCreateRepo(String repoName) {
        try {

            GetRepositoriesRequest request = new GetRepositoriesRequest();
            GetRepositoriesResponse response = getClient().snapshot().getRepository(request, RequestOptions.DEFAULT);
            List<RepositoryMetadata> repositories = response.repositories();

            if(repositories.size() == 0) {
                log.debug("No Repo's found - Creating Repo");
                return createRepo(repoName);
            } else {
                for(RepositoryMetadata repo: repositories) {
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

    public List<RepositoryMetadata> listRepos() {
        try {
            GetRepositoriesRequest request = new GetRepositoriesRequest();
            GetRepositoriesResponse response = getClient().snapshot().getRepository(request, RequestOptions.DEFAULT);
            List<RepositoryMetadata> repositories = response.repositories();

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
            DeleteSnapshotRequest request = new DeleteSnapshotRequest(repo, snapShotName);
            getClient().snapshot().delete(request, RequestOptions.DEFAULT);
        } catch (Exception ex) {
            log.error("Exception in deleteSnapShot method: " + ex.toString());
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
            getClient().snapshot().restore(request, RequestOptions.DEFAULT);

        } catch (Exception ex) {
            log.error("Exception in restoreSnapShot method: " + ex.toString());
        }
    }
    
    public void createSnapShot(String repo, String snapShotName, String index) {
        List<String> indices = new ArrayList<String>();
        indices.add(index);
        createSnapShot(repo, snapShotName, indices);
    }

    public void createSnapShot(String repo, String snapShotName, List<String> indices) {
        try {
            log.info("Creating Snapshot: " + snapShotName + " in: " + repo + " with: " + indices);

            CreateSnapshotRequest request = new CreateSnapshotRequest();
            request.repository(repo);
            request.snapshot(snapShotName);
            request.indices(indices);
            request.waitForCompletion(true);

            getClient().snapshot().create(request, RequestOptions.DEFAULT);

            log.info("Snapshot " + snapShotName + " was created for indices: " + indices);
        } catch (Exception ex){
            log.error("Exception in createSnapshot method: " + ex.toString());
        }
    }

    private String createRepo(String repoName) {

        if(repoName != null && repoName.length() > 0) {
            try {

                SiteIndexSettings settings = new SiteIndexSettings(true);
                settings.buildRepositorySettings("agr-es-backup-" + repoName);

                PutRepositoryRequest request = new PutRepositoryRequest();
                request.settings(Strings.toString(settings.getBuilder()), settings.getBuilder().contentType());
                request.name(repoName);
                request.type("s3");
                request.verify(true);
                request.timeout(new TimeValue(30, TimeUnit.MINUTES));

                log.info(repoName + " -> " + settings.toString());

                AcknowledgedResponse response = getClient().snapshot().createRepository(request, RequestOptions.DEFAULT);

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
            response = getClient().snapshot().get(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response == null) { return null; }

        return response.getSnapshots();
    }

    public List<String> getIndexList() {
        try {
            GetIndexRequest request = new GetIndexRequest("*");
            GetIndexResponse response = getClient().indices().get(request, RequestOptions.DEFAULT);
            String[] indices = response.getIndices();
            return new ArrayList<String>(Arrays.asList(indices));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void checkRepo(String repo) {
        boolean found = false;
        List<RepositoryMetadata> meta = listRepos();
        for(RepositoryMetadata data: meta) {
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

    public String getBaseIndexName() {
        return baseIndexName;
    }
    
    public void closeClient() {
        EsClientFactory.closeClient();
    }

}
