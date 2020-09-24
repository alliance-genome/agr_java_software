package org.alliancegenome.api.model.xml;

import java.util.*;

import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import lombok.*;
@Getter @Setter
@Schema(name="XMLURLSet", description="POJO that represents the XMLURLSet")
@XmlRootElement(name = "urlset")
public class XMLURLSet {

    private List<XMLURL> url = new ArrayList<XMLURL>();
}
