package org.alliancegenome.api.tests.unit;

import org.junit.Assert;
import org.junit.Test;

public class SchemaTest {

    @Test
    public void checkVersion() {
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
        Assert.assertEquals((a + b + c + d), "0099");
    }

}
