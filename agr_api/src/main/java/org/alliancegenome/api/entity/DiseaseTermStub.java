package org.alliancegenome.api.entity;

public class DiseaseTermStub {

	private String curie;
	private String name;

	public DiseaseTermStub() {
	}

	public DiseaseTermStub(String curie, String name) {
		this.curie = curie;
		this.name = name;
	}

	public String getCurie() {
		return curie;
	}

	public void setCurie(String curie) {
		this.curie = curie;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
