package org.alliancegenome.neo4j.entity.node;

import java.util.Arrays;
import java.util.Optional;

import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
@Schema(name="MITerm", description="POJO that represents the MITerm join")
public class MITerm extends Ontology {
	@JsonView({View.Interaction.class})
	private String primaryKey;
	@JsonView({View.Interaction.class})
	private String label;
	@JsonView({View.Interaction.class})
	private String definition;
	@JsonView({View.Interaction.class})
	private String url;

	@JsonView({View.Interaction.class})
	public String getDisplayName() {
		Optional<String> type = MiTermType.getNameByID(primaryKey);
		return type.orElseGet(() -> label);
	}

	@JsonView({View.Interaction.class})
	public void setDisplayName(String name) {
		//ignore
	}

	enum MiTermType {
		DS_RNA("IA:2966", "dsRNA"),
		MI_RNA("IA:2984", "miRNA"),
		GENE("MI:0250", "gene"),
		NUCLEIC_ACID("MI:0318", "nucleic acid"),
		DNA("MI:0319", "DNA"),
		RNA("MI:0320", "RNA"),
		M_RNA("MI:0324", "mRNA"),
		T_RNA("MI:0325", "tRNA"),
		PROTEIN("MI:0326", "protein"),
		PEPTIDE("MI:0327", "peptide"),
		SN_RNA("MI:0607", "snRNA"),
		R_RNA("MI:0608", "rRNA"),
		SNO_RNA("MI:0609", "snoRNA"),
		SI_RNA("MI:0610", "siRNA"),
		SRP_RNA("MI:0611", "SRP RNA"),
		DS_DNA("MI:0681", "dsDNA"),
		LINKC_RNA("MI:2190", "lincRNA");

		private String ID;
		private String name;


		MiTermType(String ID, String name) {
			this.name = name;
			this.ID = ID;
		}

		public static Optional<String> getNameByID(String id) {
			return Arrays.stream(values())
					.filter(term -> term.ID.equals(id))
					.map(miTermType -> miTermType.name)
					.findFirst();

		}
	}
}
