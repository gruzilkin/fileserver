package com.gruzilkin.blockstorage.service;

public interface BlockStorageService {
    byte[] findById(String id);
    String save(byte[] block);
}
