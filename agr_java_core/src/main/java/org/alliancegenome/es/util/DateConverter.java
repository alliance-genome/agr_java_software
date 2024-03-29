package org.alliancegenome.es.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
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

		format1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");//AllianceReleaseInfo: 2021-06-23T04:00:00.000+0000
		format2 = new SimpleDateFormat("dd:MM:yyyy HH:mm");//OntologyFileMata.date 11:05:2020 13:57
		format3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");//ModeFileMetadata: 2021-01-25T18:47:29Z

		try {
			return format1.parse(value);
		} catch (Exception e) {
			try {
				return format2.parse(value);
			} catch (Exception e1) {
				try {
				   return format3.parse(value);
				} catch (Exception e2) {
					try {
						return new Date(OffsetDateTime.parse(value).toInstant().toEpochMilli()); // This tries a bunch of other formats
					} catch (Exception e3) {
						return null;
					}
				}
			}
		}
	}

}
