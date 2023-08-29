package com.gruzilkin.fileserver.blockstorage.data.cassandra;

import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.core.mapping.Table;

import java.nio.ByteBuffer;
import java.util.UUID;

@Table
public class Block {

    @Id
    private UUID id;

    private ByteBuffer content;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ByteBuffer getContent() {
        return content;
    }

    public void setContent(ByteBuffer content) {
        this.content = content;
    }
}