package org.alliancegenome.api.model;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;

@ApiModel
@Getter @Setter
public class MetaData {
	
	private String debug;
	private String esHost;
	private String esIndex;
	private String esPort;
	
}
