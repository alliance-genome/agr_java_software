package org.alliancegenome.api.model.xml;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@XmlRootElement(name = "urlset")
public class XMLURLSet {

	private List<XMLURL> url = new ArrayList<XMLURL>();
}
