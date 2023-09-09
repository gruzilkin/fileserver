package com.gruzilkin.metadata.data.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "blocks")
public class Block {

    public Block(File file, int sort, String storageKey) {
        this.file = file;
        this.sort = sort;
        this.storageKey = storageKey;
    }

    public Block() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(name = "sort", nullable = false)
    private int sort;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }
}
