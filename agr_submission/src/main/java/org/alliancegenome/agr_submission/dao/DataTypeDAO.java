package org.alliancegenome.agr_submission.dao;

import javax.enterprise.context.ApplicationScoped;

import org.alliancegenome.agr_submission.BaseSqliteDAO;
import org.alliancegenome.agr_submission.entities.DataType;

@ApplicationScoped
public class DataTypeDAO extends BaseSqliteDAO<DataType> {

    public DataTypeDAO() {
        super(DataType.class);
    }

}
