package com.gruzilkin.fileserver.blockstorage.data.cassandra;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;

@PrimaryKeyClass
public class HashToIdKey {
    @PrimaryKeyColumn(name = "hash", type = PrimaryKeyType.PARTITIONED)
    private String hash;

    @PrimaryKeyColumn(name = "id", type = PrimaryKeyType.CLUSTERED)
    private String id;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}