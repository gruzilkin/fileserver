package com.gruzilkin.fileserver.blockstorage.data.cassandra;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("hash_id")
public class HashToId {
    @PrimaryKey
    private HashToIdKey key;

    public HashToIdKey getKey() {
        return key;
    }

    public void setKey(HashToIdKey key) {
        this.key = key;
    }
}