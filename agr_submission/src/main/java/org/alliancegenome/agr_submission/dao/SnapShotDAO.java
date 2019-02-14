package org.alliancegenome.agr_submission.dao;

import javax.enterprise.context.ApplicationScoped;

import org.alliancegenome.agr_submission.BaseSqliteDAO;
import org.alliancegenome.agr_submission.entities.SnapShot;

@ApplicationScoped
public class SnapShotDAO extends BaseSqliteDAO<SnapShot> {

    public SnapShotDAO() {
        super(SnapShot.class);
    }

}
