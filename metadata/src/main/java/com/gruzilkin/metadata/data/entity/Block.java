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
}
