package com.gruzilkin.fileserver.blockstorage.data.cassandra.repository;

import com.gruzilkin.fileserver.blockstorage.data.cassandra.HotBlock;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.nio.ByteBuffer;
import java.time.Instant;

public interface HotBlockRepository extends CrudRepository<HotBlock, String> {
    @Query("INSERT INTO hot_block (id, content, hash, update_date) VALUES (:id, :content, :hash, :updateDate) USING TTL :ttl")
    void saveWithTtl(@Param("id") String id, @Param("content") ByteBuffer content, @Param("hash") String hash, @Param("updateDate") Instant updateDate, @Param("ttl") int ttl);
}
