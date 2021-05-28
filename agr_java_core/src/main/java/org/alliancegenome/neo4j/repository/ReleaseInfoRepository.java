package org.alliancegenome.neo4j.repository;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.neo4j.entity.node.AllianceReleaseInfo;

@RequestScoped
public class ReleaseInfoRepository extends Neo4jRepository<AllianceReleaseInfo> {

    public ReleaseInfoRepository() {
        super(AllianceReleaseInfo.class);
    }

}
