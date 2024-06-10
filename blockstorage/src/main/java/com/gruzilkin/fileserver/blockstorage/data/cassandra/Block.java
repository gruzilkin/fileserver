package com.gruzilkin.fileserver.blockstorage.data.cassandra;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.nio.ByteBuffer;
import java.time.Instant;

@Table("block")
public class Block {
    @PrimaryKey
    private String id;

    @Column
    private ByteBuffer content;

    @Column
    private String hash;

    @Column("update_date")
    private Instant updateDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ByteBuffer getContent() {
        return content;
    }

    public void setContent(ByteBuffer content) {
        this.content = content;
    }

    public String getHash() { return hash; }

    public void setHash(String hash) { this.hash = hash; }

    public Instant getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Instant updateDate) {
        this.updateDate = updateDate;
    }
}