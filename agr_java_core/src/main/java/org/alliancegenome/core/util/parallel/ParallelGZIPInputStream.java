package org.alliancegenome.core.util.parallel;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class ParallelGZIPInputStream extends GZIPInputStream {

    public ParallelGZIPInputStream(InputStream in, int size) throws IOException {
        super(in, size);
    }

    public ParallelGZIPInputStream(InputStream in) throws IOException {
        super(in);
    }
}