package org.alliancegenome.cacher.cachers;

import org.infinispan.client.hotrod.RemoteCache;

public abstract class IOCacher<I, O> extends Cacher {

    protected RemoteCache<String, I> inputCache;
    protected RemoteCache<String, O> outputCache;
    
    public IOCacher(String inputCacheName, String outputCacheName) {
        inputCache = setupCache(inputCacheName);
        outputCache = setupCache(outputCacheName);
    }

    protected abstract void cache();

}
