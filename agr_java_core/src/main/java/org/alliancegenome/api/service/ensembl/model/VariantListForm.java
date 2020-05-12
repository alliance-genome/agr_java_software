package org.alliancegenome.api.service.ensembl.model;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class VariantListForm {
    @JsonView(View.Default.class)
    private List<String> ids = new ArrayList<String>();
}
