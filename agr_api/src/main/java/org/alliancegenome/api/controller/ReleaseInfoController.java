package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.rest.interfaces.ReleaseInfoRESTInterface;
import org.alliancegenome.api.service.ReleaseInfoService;
import org.alliancegenome.neo4j.entity.ReleaseSummary;
import org.alliancegenome.neo4j.entity.node.AllianceReleaseInfo;

@RequestScoped
public class ReleaseInfoController implements ReleaseInfoRESTInterface {

    @Inject ReleaseInfoService releaseService;

    @Override
    public AllianceReleaseInfo getReleaseInfo() {
        return releaseService.getReleaseInfo();
    }

    @Override
    public ReleaseSummary getReleaseInfoSummary() {
        return releaseService.getSummary();
    }

}
