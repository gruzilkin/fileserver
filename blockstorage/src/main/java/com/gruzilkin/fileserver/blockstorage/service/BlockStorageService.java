package com.gruzilkin.fileserver.blockstorage.service;

public interface BlockStorageService {
    class BlockDescription {
        public final String id;
        public final String hash;

        public BlockDescription(String id, String hash) {
            this.id = id;
            this.hash = hash;
        }
    }

    byte[] findById(String id);
    BlockDescription save(byte[] block);
    void commit(String id);
}
