package org.alliancegenome.es.index.data.dao;

import java.io.File;
import java.util.Date;

import org.alliancegenome.aws.S3Helper;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.exceptions.GenericException;
import org.alliancegenome.es.index.dao.ESDocumentDAO;
import org.alliancegenome.es.index.data.document.DataFileDocument;
import org.alliancegenome.es.index.data.document.DataTypeDoclet;
import org.alliancegenome.es.index.data.document.MetaDataDocument;
import org.alliancegenome.es.index.data.document.TaxonIdDoclet;
import org.alliancegenome.es.index.data.enums.DataType;
import org.alliancegenome.es.index.data.enums.TaxonIdType;
import org.alliancegenome.github.GithubRESTAPI;
import org.alliancegenome.github.model.GithubRelease;
import org.alliancegenome.github.util.GitHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class MetaDataDAO extends ESDocumentDAO<MetaDataDocument> {
	
	private Log log = LogFactory.getLog(getClass());
	
	private GithubRESTAPI githubAPI = new GithubRESTAPI();

	private static GitHelper gitHelper = new GitHelper();
	private static S3Helper s3Helper = new S3Helper();
	
	private MetaDataDocument metaData;
	
	private DataFileDAO dataFileDAO = new DataFileDAO();
	
	public MetaDataDAO() {
		log.debug("Checking Data Index");
		checkIndex(ConfigHelper.getEsDataIndex());
		getMetaDocument();
		if(metaData.getSchemas() == null || metaData.getSchemas().size() == 0) {
			getLatestSchemaVersion();
		}
		checkDataTypes();
		checkTaxonIds();
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

	public String getLatestSchemaVersion() {
		getMetaDocument();
		
		String githubLatestRelease = githubAPI.getLatestRelease("agr_schemas").getName();
		log.debug("Getting Latest Schema Version");
		
		if(!metaData.getSchemas().contains(githubLatestRelease)) {
			metaData.getSchemas().add(githubLatestRelease);
			updateDocument(metaData);
		}
		log.debug("Schema Version: " + githubLatestRelease);
		return githubLatestRelease;
	}

	// Null -> Returns latest schema from github
	// Invalid schema -> null
	// Schema not in ES but in Github -> schema version
	public String getSchemaVersion(String string) {
		getMetaDocument();
		log.debug("Getting Schema Version");
		if(string != null) {

			if(!metaData.getSchemas().contains(string)) {
				GithubRelease gitHubSchema = githubAPI.getRelease("agr_schemas", string);

				if(gitHubSchema != null) {
					metaData.getSchemas().add(gitHubSchema.getName());
					updateDocument(metaData);
					return gitHubSchema.getName();
				} else {
					log.debug("Github Schema Version was null: " + gitHubSchema);
					return null;
				}
			}
			log.debug("Schema Found: " + string);
			return string;
		} else {
			log.debug("Null Schema version");
			return getLatestSchemaVersion();
		}
	}
	
	public TaxonIdDoclet getTaxonIdDocument(String string) {
		log.debug("Getting TaxonId: " + string);
		if(TaxonIdType.fromTaxonId(string) != null) {
			return TaxonIdType.fromTaxonId(string);
		}
		if(TaxonIdType.fromModName(string) != null) {
			return TaxonIdType.fromModName(string);
		}
		return null;
	}

	private void checkTaxonIds() {
		getMetaDocument();
		for(TaxonIdType t: TaxonIdType.values()) {
			log.trace("TaxonId: " + t.getTaxonId());
			if(metaData.getTaxonIds().get(t.getTaxonId()) == null) {
				log.trace("Creating TaxonId in ES: " + t.getTaxonId());
				metaData.getTaxonIds().put(t.getTaxonId(), TaxonIdType.getDoclet(t));
				updateDocument(metaData);
			}
		}
	}

	public String saveFileToS3(String schemaVersion, DataTypeDoclet dataType, String bodyString) throws GenericException {
		int fileIndex = s3Helper.listFiles(schemaVersion + "/" + dataType.getName() + "/");
		String filePath = schemaVersion + "/" + dataType.getName() + "/" + schemaVersion + "_" + dataType.getName() + "_" + fileIndex + "." + dataType.getFileExtension();
		s3Helper.saveFile(filePath, bodyString);
		return filePath;
	}

	public String saveFileToS3(String schemaVersion, DataTypeDoclet dataType, TaxonIdDoclet taxon, String bodyString) throws GenericException {
		int fileIndex = s3Helper.listFiles(schemaVersion + "/" + dataType.getName() + "/" + taxon.getTaxonId() + "/");

		String filePath =
				schemaVersion + "/" + dataType.getName() + "/" + taxon.getTaxonId() + "/" +
						schemaVersion + "_" + dataType.getName() + "_" + taxon.getTaxonId() + "_" + fileIndex + "." + dataType.getFileExtension();

		s3Helper.saveFile(filePath, bodyString);
		return filePath;
	}

	public File getSchemaFile(String schemaVersionName, String dataTypeFilePath) {
		return gitHelper.getFile(schemaVersionName, dataTypeFilePath);
	}

	public void createDataFile(String schemaVersion, DataTypeDoclet dataType, TaxonIdDoclet taxon, String filePath) {
		DataFileDocument df = new DataFileDocument();
		df.setDataType(dataType.getName());
		df.setPath(filePath);
		df.setSchemaVersion(schemaVersion);
		df.setTaxonId(taxon.getTaxonId());
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
}
