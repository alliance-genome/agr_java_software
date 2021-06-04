package org.alliancegenome.es.util;

import java.text.*;
import java.util.Date;

import org.neo4j.ogm.typeconversion.AttributeConverter;

public class DateConverter implements AttributeConverter<Date, String> {

    private DateFormat format1;
    private DateFormat format2;
    private DateFormat format3;
    
    @Override
    public String toGraphProperty(Date value) {
        return value.toString();
    }

    @Override
    public Date toEntityAttribute(String value) {

    	//allele: 2021-02-22T18:39:40-05:00    	
        format1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");//
    	format2 = new SimpleDateFormat("dd:MM:yyyy HH:mm");//OntologyFileMata.date 11:05:2020 13:57
        format3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");//ModeFileMetadata: 2021-01-25T18:47:29Z


        try {
            return format1.parse(value);
            //return new Date();
        } catch (Exception e) {
            try {
                return format2.parse(value);
            } catch (Exception e1) {
            	try {
                   return format3.parse(value);
                } catch (Exception e2) {
                    return null;
                }
            }
        }
    }

    //    
}
