package org.alliancegenome.api.entity;

import java.io.Serializable;

import org.alliancegenome.core.view.PublicView;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionSlim implements Serializable {

	@JsonView({PublicView.DiseaseAnnotation.class, PublicView.Expression.class})
	private String id;
	@JsonView({PublicView.DiseaseAnnotation.class, PublicView.Expression.class})
	private String label;
	@JsonView({PublicView.DiseaseAnnotation.class, PublicView.Expression.class})
	private String description;
	@JsonView({PublicView.DiseaseAnnotation.class, PublicView.Expression.class})
	private String type = "Term";

	public void setTypeAll() {
		type = Type.ALL.getDisplayName();
	}

	public void setTypeOther() {
		type = Type.OTHER.getDisplayName();
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
