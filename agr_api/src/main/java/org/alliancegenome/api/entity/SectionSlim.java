package org.alliancegenome.api.entity;

import java.io.Serializable;

import org.alliancegenome.core.view.PublicView;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;

@Data
@JsonView({PublicView.DiseaseAnnotation.class, PublicView.Expression.class})
public class SectionSlim implements Serializable {
	
	private String id;
	private String label;
	private String description;
	private String type = "Term";

	public void setTypeAll() {
		type = Type.ALL.getDisplayName();
	}

	public enum Type {
		TERM("Term"), ALL("All"), OTHER("Other");

		private String displayName;

		Type(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}
	}

	@Override
	public String toString() {
		return id + ':' + label;
	}
}
