package org.alliancegenome.view;

import java.util.HashMap;

import org.alliancegenome.es.model.query.FieldFilter;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BaseFilter extends HashMap<FieldFilter, String> {

}
