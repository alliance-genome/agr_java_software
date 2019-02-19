package org.alliancegenome.agr_submission.services;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.alliancegenome.agr_submission.BaseService;
import org.alliancegenome.agr_submission.dao.DataFileDAO;
import org.alliancegenome.agr_submission.entities.DataFile;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class DataFileService extends BaseService<DataFile> {

    @Inject
    private DataFileDAO dao;

    @Override
    @Transactional
    public DataFile create(DataFile entity) {
        log.info("DataFileService: create: ");
        return dao.persist(entity);
    }

    @Override
    @Transactional
    public DataFile get(Long id) {
        log.info("DataFileService: get: " + id);
        return dao.find(id);
    }

    @Override
    @Transactional
    public DataFile update(DataFile entity) {
        log.info("DataFileService: update: ");
        return dao.merge(entity);
    }

    @Override
    @Transactional
    public DataFile delete(Long id) {
        log.info("DataFileService: delete: " + id);
        return dao.remove(id);
    }

    public List<DataFile> getDataFiles() {
        return dao.findAll();
    }


}
