package org.alliancegenome.api.response;

import org.alliancegenome.core.view.PublicView;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;

@Data
@JsonView(PublicView.Default.class)
class Request {
	private String uri;
}
