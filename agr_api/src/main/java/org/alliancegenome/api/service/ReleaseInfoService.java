package org.alliancegenome.api.service;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.neo4j.entity.ReleaseSummary;
import org.alliancegenome.neo4j.entity.node.AllianceReleaseInfo;
import org.alliancegenome.neo4j.repository.ModFileRepository;
import org.alliancegenome.neo4j.repository.OntologyFileRepository;
import org.alliancegenome.neo4j.repository.ReleaseInfoRepository;

@RequestScoped
public class ReleaseInfoService {

	private static ReleaseInfoRepository releaseRepo = new ReleaseInfoRepository();
	
	@Inject ModFileRepository modFileRepo;
	@Inject OntologyFileRepository ontologyFileRepo;

	public AllianceReleaseInfo getReleaseInfo() {
		return StreamSupport.stream(releaseRepo.getAll().spliterator(), false).collect(Collectors.toList()).get(0);
	}

	public ReleaseSummary getSummary() {
		ReleaseSummary sum = new ReleaseSummary();
		sum.setReleaseInfo(StreamSupport.stream(releaseRepo.getAll().spliterator(), false).collect(Collectors.toList()).get(0));
		sum.setMetaData(StreamSupport.stream(modFileRepo.getAll().spliterator(), false).collect(Collectors.toList()));
		sum.setOntologyMetaData(StreamSupport.stream(ontologyFileRepo.getAll().spliterator(), false).collect(Collectors.toList()));
		return sum;
	}

}
