package com.gruzilkin.fileserver.web;

import com.gruzilkin.common.BlockStorageServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BlockStorageStubFactory {
    private final ManagedChannel managedChannel;

    public BlockStorageStubFactory(@Value("${block-storage-host}") String host,
                                   @Value("${block-storage-port}") int port) {
        managedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    }


    public BlockStorageServiceGrpc.BlockStorageServiceBlockingStub getBlockStorage() {
        return BlockStorageServiceGrpc.newBlockingStub(managedChannel);
    }

    public BlockStorageServiceGrpc.BlockStorageServiceFutureStub getBlockStoragAsync() {
        return BlockStorageServiceGrpc.newFutureStub(managedChannel);
    }
}
