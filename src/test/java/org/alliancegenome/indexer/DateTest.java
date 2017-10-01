package org.alliancegenome.indexer;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DateTest {

    public static void main(String[] args) throws ParseException {
        String date = "2009-07-16T19:20:30-05:00";
        //String date = "2017-08-17T07:45:21.234-07:00";
        String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
        DateFormat format = new SimpleDateFormat(pattern);
        //DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
        //DateTime dateTime = dtf.parseDateTime(date);
        System.out.println(format.parse(date));

    }

}
