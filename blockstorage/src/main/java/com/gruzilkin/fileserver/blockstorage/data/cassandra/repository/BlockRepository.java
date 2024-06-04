package com.gruzilkin.fileserver.blockstorage.data.cassandra.repository;

import com.gruzilkin.fileserver.blockstorage.data.cassandra.Block;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.nio.ByteBuffer;
import java.time.Instant;

public interface BlockRepository extends CrudRepository<Block, String> {
    @Query("INSERT INTO block (id, content, update_date) VALUES (:id, :content, :updateDate) USING TTL :ttl")
    void saveWithTtl(@Param("id") String id, @Param("content") ByteBuffer content, @Param("updateDate") Instant updateDate, @Param("ttl") int ttl);
}
