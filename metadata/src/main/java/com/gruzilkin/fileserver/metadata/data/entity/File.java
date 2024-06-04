package com.gruzilkin.fileserver.metadata.data.entity;


import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "files")
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OrderBy("sort ASC")
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "file", cascade = CascadeType.ALL)
    private List<Block> blocks;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }
}
