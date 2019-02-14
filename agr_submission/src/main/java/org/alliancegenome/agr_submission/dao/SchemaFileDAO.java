package org.alliancegenome.agr_submission.dao;

import javax.enterprise.context.ApplicationScoped;

import org.alliancegenome.agr_submission.BaseSqliteDAO;
import org.alliancegenome.agr_submission.entities.SchemaFile;

@ApplicationScoped
public class SchemaFileDAO extends BaseSqliteDAO<SchemaFile> {

    public SchemaFileDAO() {
        super(SchemaFile.class);
    }

}
