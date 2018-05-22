package org.alliancegenome.api;

public class SchemaTest {

    public static void main(String[] args) {
        String schemaVersion = "0.1.0.0";
        
        String[] array = schemaVersion.split("\\.");
        
        
        int out = Integer.parseInt(array[0] + array[1] + array[2] + array[3]);
        out--;
        
        String a = (out / 1000) + "";
        out = out % 1000;
        String b = (out / 100) + "";
        out = out % 100;
        String c = (out / 10) + "";
        out = out % 10;
        String d = out + "";
        
        System.out.println(a + b + c + d);
        
        //return a + "." + b + "." + c + "." + d;
    }

}
