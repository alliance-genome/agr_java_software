package org.alliancegenome.cacher.cachers;

import org.infinispan.client.hotrod.RemoteCache;

public abstract class DBCacher<T> extends Cacher {

    protected RemoteCache<String, T> cache;

    public DBCacher(String cacheName) {
        cache = setupCache(cacheName);
    }

    protected abstract void cache();
}
