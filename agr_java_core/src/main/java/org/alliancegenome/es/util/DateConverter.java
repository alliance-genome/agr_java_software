package org.alliancegenome.es.util;

import java.text.*;
import java.util.Date;

import org.neo4j.ogm.typeconversion.AttributeConverter;

public class DateConverter implements AttributeConverter<Date, String> {

    private DateFormat format1;
    private DateFormat format2;
    
    @Override
    public String toGraphProperty(Date value) {
        return value.toString();
    }

    @Override
    public Date toEntityAttribute(String value) {
        format1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        format2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        try {
            return format1.parse(value);
        } catch (Exception e) {
            try {
                return format2.parse(value);
            } catch (Exception e1) {
                return null;
            }
        }
    }

    //    
}
