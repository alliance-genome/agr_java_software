package org.alliancegenome.neo4j.repository;

import org.alliancegenome.neo4j.entity.node.AllianceReleaseInfo;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class ReleaseInfoRepository extends Neo4jRepository<AllianceReleaseInfo> {

	public ReleaseInfoRepository() {
		super(AllianceReleaseInfo.class);
	}

}
