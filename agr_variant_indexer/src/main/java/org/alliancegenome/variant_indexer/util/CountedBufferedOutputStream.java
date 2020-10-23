package org.alliancegenome.variant_indexer.util;

import java.io.*;

import lombok.*;

@Getter @Setter
public class CountedBufferedOutputStream extends BufferedOutputStream {

    private long byteCount;
    
    public CountedBufferedOutputStream(OutputStream out) {
        super(out);
        byteCount = 0;
    }
    
    public CountedBufferedOutputStream(OutputStream out, int size) {
        super(out, size);
        byteCount = 0;
    }

    @Override
    public void write(byte[] b) throws IOException {
        byteCount += b.length;
        super.write(b);
    }

}
