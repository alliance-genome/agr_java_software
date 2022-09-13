package org.alliancegenome.indexer.variant.scripts;

import java.util.*;

import org.alliancegenome.api.json.APIBeanSerializerModifier;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.*;

import lombok.Data;

public class TestNullSerializer {

	public static void main(String[] args) throws Exception {
		
		ObjectMapper mapper = new ObjectMapper();

		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.setSerializationInclusion(Include.NON_NULL);

		mapper.setSerializerFactory(mapper.getSerializerFactory().withSerializerModifier(new APIBeanSerializerModifier()));
		
		CoolThing c = new CoolThing();
		System.out.println(mapper.writeValueAsString(c));
	}

}

@Data
class CoolThing {
	private String name = "";
	private String name2;
	private String name3 = "Fred";
	private ArrayList<String> list;
	private HashMap<String, String> map;
	private ArrayList<String> list2;
	private HashMap<String, String> map2;
	private HashSet<String> set;
	private HashSet<String> set2;

	public CoolThing() {
		set = new HashSet<String>();
		list = new ArrayList<String>();
		map = new HashMap<String, String>();
	}
}
