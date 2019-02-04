package org.alliancegenome.es.index.data.dao;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

import org.alliancegenome.aws.S3Helper;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.exceptions.GenericException;
import org.alliancegenome.es.index.ESDocumentDAO;
import org.alliancegenome.es.index.data.doclet.DataTypeDoclet;
import org.alliancegenome.es.index.data.doclet.SnapShotDoclet;
import org.alliancegenome.es.index.data.document.DataFileDocument;
import org.alliancegenome.es.index.data.document.DataSnapShotDocument;
import org.alliancegenome.es.index.data.document.MetaDataDocument;
import org.alliancegenome.es.index.data.enums.DataType;
import org.alliancegenome.es.index.site.doclet.SpeciesDoclet;
import org.alliancegenome.github.GitHelper;
import org.alliancegenome.github.GithubRESTAPI;
import org.alliancegenome.github.GithubRelease;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class MetaDataDAO extends ESDocumentDAO<MetaDataDocument> {
    
    private Log log = LogFactory.getLog(getClass());
    
    private GithubRESTAPI githubAPI = new GithubRESTAPI();

    private static GitHelper gitHelper = new GitHelper();
    private static S3Helper s3Helper = new S3Helper();
    
    private MetaDataDocument metaData;
    
    private DataFileDAO dataFileDAO = new DataFileDAO();
    private DataSnapShotDAO dataSnapShotDAO = new DataSnapShotDAO();
    
    public MetaDataDAO() {
        log.debug("Checking Data Index");
        checkIndex(ConfigHelper.getEsDataIndex());
        getMetaDocument();
        checkForSchemas();
        checkDataTypes();
        checkSpecies();
    }

    private void checkForSchemas() {
        if(metaData.getSchemas() == null || metaData.getSchemas().size() == 0) {
            log.debug("Getting Latest Schema Version");
            String githubLatestRelease = githubAPI.getLatestRelease("agr_schemas").getTag_name();
            metaData.getSchemas().add(githubLatestRelease);
            updateDocument(metaData);
            log.debug("Schema Version: " + githubLatestRelease);
        }
    }

    // Null -> return null
    // Invalid data type -> null
    // Data Type not in ES but in Valid in ENUM -> data type plus inject document
    public DataTypeDoclet getDataType(String string) {
        log.debug("Getting Data Type: " + string);
        if(string != null && metaData.getDataTypes().containsKey(string)) {
            return metaData.getDataTypes().get(string);
        }
        return null;
    }
    
    private void checkDataTypes() {
        getMetaDocument();
        for(DataType d: DataType.values()) {
            log.trace("Data Type: " + d);
            if(metaData.getDataTypes().get(d.name()) == null) {
                log.trace("Creating Datatype in ES: " + d);
                metaData.getDataTypes().put(d.name(), DataType.getDoclet(d));
                updateDocument(metaData);
            }
        }
    }
    
    public String getCurrentSchemaVersion() {
        getMetaDocument();
        if(metaData.getCurrentRelease().length() > 0) {
            return metaData.getReleaseSchemaMap().get(metaData.getCurrentRelease());
        } else {
            log.warn("Current Release Version is not Set on metadata document");
            return null;
        }
    }
    
    // Null -> Returns current schema from release marked current
    // Invalid schema -> null
    // Schema not in ES but in Github -> schema version
    public String getSchemaVersion(String string) {
        getMetaDocument();
        log.debug("Getting Schema Version");
        if(string != null) {

            if(!metaData.getSchemas().contains(string)) {
                GithubRelease gitHubSchema = githubAPI.getRelease("agr_schemas", string);

                if(gitHubSchema != null) {
                    metaData.getSchemas().add(gitHubSchema.getTag_name());
                    updateDocument(metaData);
                    return gitHubSchema.getTag_name();
                } else {
                    log.debug("Github Schema Version was null: " + gitHubSchema);
                    return null;
                }
            }
            log.debug("Schema Found: " + string);
            return string;
        } else {
            log.debug("Null Schema version");
            return getCurrentSchemaVersion();
        }
    }
    
    public SpeciesDoclet getSpeciesDoclet(String string) {
        log.debug("Getting TaxonId: " + string);
        return SpeciesType.getByModNameOrIdPart(string);
    }

    private void checkSpecies() {
        getMetaDocument();
        
        for(SpeciesType s: SpeciesType.values()) {
            log.trace("TaxonId: " + s.getTaxonID());
            if(metaData.getSpecies().get(s.getTaxonID()) == null) {
                log.trace("Creating TaxonId in ES: " + s.getTaxonID());
                metaData.getSpecies().put(s.getTaxonID(), s.getDoclet());
                updateDocument(metaData);
            }
        }
    }

    public String saveFileToS3(String schemaVersion, DataTypeDoclet dataType, File inFile) throws GenericException {
        int fileIndex = s3Helper.listFiles(schemaVersion + "/" + dataType.getName() + "/");
        String filePath = schemaVersion + "/" + dataType.getName() + "/" + schemaVersion + "_" + dataType.getName() + "_" + fileIndex + "." + dataType.getFileExtension();
        s3Helper.saveFile(filePath, inFile);
        return filePath;
    }

    public String saveFileToS3(String schemaVersion, DataTypeDoclet dataType, SpeciesDoclet species, File inFile) throws GenericException {
        int fileIndex = s3Helper.listFiles(schemaVersion + "/" + dataType.getName() + "/" + species.getTaxonIDPart() + "/");

        String filePath =
                schemaVersion + "/" + dataType.getName() + "/" + species.getTaxonIDPart() + "/" +
                        schemaVersion + "_" + dataType.getName() + "_" + species.getTaxonIDPart() + "_" + fileIndex + "." + dataType.getFileExtension();

        s3Helper.saveFile(filePath, inFile);
        return filePath;
    }

    public File getSchemaFile(String schemaVersionName, String dataTypeFilePath) {
        return gitHelper.getFile(schemaVersionName, dataTypeFilePath);
    }

    public void createDataFile(String schemaVersion, DataTypeDoclet dataType, SpeciesDoclet species, String filePath) {
        DataFileDocument df = new DataFileDocument();
        df.setDataType(dataType.getName());
        df.setS3path(filePath);
        df.setSchemaVersion(schemaVersion);
        if(species != null) {
            df.setTaxonIDPart(species.getTaxonIDPart());
        }
        df.setUploadDate(new Date());
        dataFileDAO.createDocumnet(df);
    }
    
    private void getMetaDocument() {
        metaData = readDocument("meta_data_id", "meta_data");
        if(metaData == null) {
            metaData = new MetaDataDocument();
            createDocumnet(metaData);
        }
    }
    
    private DataSnapShotDocument getShapShotDocument(String system) {
        if(system == null) return null;
        DataSnapShotDocument dsd = dataSnapShotDAO.readDocument(system, "data_snapshot");
        if(dsd == null) {
            log.debug("Document Does not exist creating it: " + dsd);
            dsd = new DataSnapShotDocument(system);
            dataSnapShotDAO.createDocumnet(dsd);
        }
        return dsd;
    }

    public String getPreviousVersion(String version) {
        String[] array = version.split("\\.");
        int out = Integer.parseInt(array[0] + array[1] + array[2] + array[3]);
        if(out <= 0) return null;
        out--;
        String a = (out / 1000) + "";
        out = out % 1000;
        String b = (out / 100) + "";
        out = out % 100;
        String c = (out / 10) + "";
        out = out % 10;
        String d = out + "";
        return a + "." + b + "." + c + "." + d;
    }

    public SnapShotDoclet getSnapShot(String system, String releaseVersion) {
        getMetaDocument();
        DataSnapShotDocument dsd = getShapShotDocument(system);
        log.debug("getSnapShot: " + dsd);
        SnapShotDoclet doc = new SnapShotDoclet();
        doc.setReleaseVersion(releaseVersion);
        doc.setSchemaVersion(metaData.getReleaseSchemaMap().get(releaseVersion));
        doc.setSnapShotDate(dsd.getReleaseSnapShotMap().get(releaseVersion));
        doc.setDataFiles(dataFileDAO.search(doc.getSchemaVersion(), doc.getSnapShotDate()));
        return doc;
    }

    public HashMap<String, Date> getReleases(String system) {
        if(system == null) system = "production";
        DataSnapShotDocument dsd = getShapShotDocument(system);
        return dsd.getReleaseSnapShotMap();
    }

    public SnapShotDoclet takeSnapShot(String system, String releaseVersion) {
        if(system == null) system = "production";
        DataSnapShotDocument dsd = getShapShotDocument(system);
        log.debug("takeSnapShot: " + dsd);
        dsd.getReleaseSnapShotMap().put(releaseVersion, new Date());
        log.debug("takeSnapShot: " + dsd);
        dataSnapShotDAO.updateDocument(dsd);
        log.debug("takeSnapShot: " + dsd);
        return getSnapShot(system, releaseVersion);
    }
}
