package com.gruzilkin.blockstorage.data.cassandra;

import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.core.mapping.Table;

import java.nio.ByteBuffer;

@Table
public class Block {

    @Id
    private String id;

    private ByteBuffer content;

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
}