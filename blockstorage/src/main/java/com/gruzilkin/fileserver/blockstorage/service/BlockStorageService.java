package com.gruzilkin.fileserver.blockstorage.service;

public interface BlockStorageService {
    byte[] findById(String id);
    String save(byte[] block);
    void commit(String id);
}
