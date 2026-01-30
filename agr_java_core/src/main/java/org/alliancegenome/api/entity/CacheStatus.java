package org.alliancegenome.api.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.neo4j.view.PublicView;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonPropertyOrder({"name", "entitiesInCache", "speciesStats", "entityStats"})
public class CacheStatus implements Serializable {

	@JsonView(PublicView.Cacher.class)
	private String name;
	@JsonView(PublicView.Cacher.class)
	private int numberOfEntityIDs;
	@JsonIgnore
	private int numberOfEntities;
	@JsonView(PublicView.CacherDetail.class)
	Map<String, Integer> entityStats;
	@JsonView(PublicView.Cacher.class)
	Map<String, Integer> speciesStats;
	@JsonView(PublicView.Cacher.class)
	private String entitiesInCache;
	@JsonView(PublicView.Cacher.class)
	private String collectionEntity;
	@JsonView(PublicView.Cacher.class)
	private String jsonViewClass;
	@JsonView(PublicView.Cacher.class)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")
	private Date dateCreated = new Date();
	@JsonIgnore
	private CacheAlliance cache;

	public CacheStatus(CacheAlliance cache) {
		this.cache = cache;
		this.name = cache.getCacheName();
	}

	public CacheStatus() {
	}

	public void setNumberOfEntities(int numberOfEntities) {
		this.numberOfEntities = numberOfEntities;
		this.entitiesInCache = String.format("%,d", numberOfEntities);
	}
}
