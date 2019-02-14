package org.alliancegenome.agr_submission.dao;

import javax.enterprise.context.ApplicationScoped;

import org.alliancegenome.agr_submission.BaseSqliteDAO;
import org.alliancegenome.agr_submission.entities.DataFile;

@ApplicationScoped
public class DataFileDAO extends BaseSqliteDAO<DataFile> {

    public DataFileDAO() {
        super(DataFile.class);
    }

}
