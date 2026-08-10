package org.alliancegenome.api.model.xml;

import java.io.Serializable;
import java.util.Date;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@XmlRootElement(name = "url")
public class XMLURL implements Serializable {

	private String loc;
	private Date lastmod;
	private String changefreq;
	private String priority;

}
