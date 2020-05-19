package org.alliancegenome.api.model.xml;

import java.io.Serializable;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@Schema(name="XMLURL", description="POJO that represents the XMLURL")
@XmlRootElement(name = "url")
public class XMLURL implements Serializable {

    private String loc;
    private Date lastmod;
    private String changefreq;
    private String priority;

}
