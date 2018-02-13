package org.alliancegenome.shared.es.dao.data_index;

import org.alliancegenome.shared.es.dao.ESDocumentDAO;
import org.alliancegenome.shared.es.document.data_index.SchemaDocument;
import org.alliancegenome.shared.github.GithubRESTAPI;
import org.alliancegenome.shared.github.model.GithubRelease;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SchemaDAO extends ESDocumentDAO<SchemaDocument> {

	private Log log = LogFactory.getLog(getClass());

	private GithubRESTAPI githubAPI = new GithubRESTAPI();

	public SchemaDocument getLatestSchemaVersion() {
		GithubRelease githubLatestRelease = githubAPI.getLatestRelease("agr_schemas");
		log.debug("Getting Latest Schema Version");
		SchemaDocument schemaVersion = readDocument(githubLatestRelease.getName(), "schema");
		log.debug("Schema Version: " + schemaVersion);
		if(schemaVersion == null) {
			schemaVersion = new SchemaDocument();
			schemaVersion.setName(githubLatestRelease.getName());
			createDocumnet(schemaVersion);
		}
		return schemaVersion;
	}

	// Null -> Returns latest schema from github
	// Invalid schema -> null
	// Schema not in ES but in Github -> schema version
	public SchemaDocument getSchemaVersion(String string) {
		log.debug("Getting Schema Version");
		if(string != null) {
			SchemaDocument schemaVersion = readDocument(string, "schema");
			log.debug("Schema Version: " + schemaVersion);
			if(schemaVersion == null) {
				GithubRelease gitHubSchema = githubAPI.getRelease("agr_schemas", string);

				if(gitHubSchema != null) {
					schemaVersion = new SchemaDocument();
					schemaVersion.setName(gitHubSchema.getName());
					createDocumnet(schemaVersion);
					return schemaVersion;
				} else {
					log.debug("Github Schema Version was null: " + gitHubSchema);
					return null;
				}
			}
			return schemaVersion;
		} else {
			log.debug("Null Schema version");
			return getLatestSchemaVersion();
		}

	}


}
