package com.gruzilkin.fileserver.blockstorage.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
    private final BlockStorageService blockStorageService;

    public KafkaConsumerService(BlockStorageService blockStorageService) {
        this.blockStorageService = blockStorageService;
    }

    @KafkaListener(topics = "block-created", groupId = "fileserver")
    public void listen(String message) {
        System.out.println("Received Message: " + message);
        blockStorageService.commit(message);
    }
}