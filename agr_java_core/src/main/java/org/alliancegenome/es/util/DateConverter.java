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
        // standard format 2020-07-23T22:26:03-07:00
        
        // 2021-02-25T14:05:06-05:00
        // 2021-06-23T04:00:00Z
        // 2021-05-27T13:18:54.614000000Z
        
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
