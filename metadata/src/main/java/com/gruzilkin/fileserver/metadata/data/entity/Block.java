package com.gruzilkin.fileserver.metadata.data.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "blocks")
public class Block {
    public Block(String id, File file, int sort) {
        this.id = id;
        this.file = file;
        this.sort = sort;
    }

    public Block() {
    }

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(name = "sort", nullable = false)
    private int sort;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }
}
