package org.alliancegenome.api.json;

import java.util.List;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

public class APIBeanSerializerModifier extends BeanSerializerModifier {

	private JsonSerializer _nullValueJsonSerializer = new NullValueJsonSerializer();
	private JsonSerializer _nullArrayJsonSerializer = new NullArrayJsonSerializer();
	private JsonSerializer _nullObjectJsonSerializer = new NullObjectJsonSerializer();

	@Override
	public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List beanProperties) {
		for (int i = 0; i < beanProperties.size(); i++) {
			BeanPropertyWriter writer = (BeanPropertyWriter) beanProperties.get(i);
			if (isArrayType(writer)) {
				//Register the writer with its own nullSerializer
				writer.assignNullSerializer(this._nullArrayJsonSerializer);
			} else if(isObjectType(writer)) {
				writer.assignNullSerializer(this._nullObjectJsonSerializer);
			} else {
				// Write null as "null" or ""
				//writer.assignNullSerializer(this._nullValueJsonSerializer);
			}
		}
		return beanProperties;
	}
	
	protected boolean isArrayType(BeanPropertyWriter writer) {
		JavaType type = writer.getType();
		return type.isArrayType() || type.isCollectionLikeType();
	}
	
	protected boolean isObjectType(BeanPropertyWriter writer) {
		JavaType type = writer.getType();
		return type.isJavaLangObject() || type.isMapLikeType();
	}
}
