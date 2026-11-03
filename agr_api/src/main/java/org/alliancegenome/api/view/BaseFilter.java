package org.alliancegenome.api.view;

import java.util.HashMap;

import org.alliancegenome.api.es.query.FieldFilter;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BaseFilter extends HashMap<FieldFilter, String> {

}
