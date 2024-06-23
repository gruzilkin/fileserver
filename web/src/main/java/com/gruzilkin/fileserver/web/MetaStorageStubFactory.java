package com.gruzilkin.fileserver.web;

import com.gruzilkin.fileserver.common.MetaStorageServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MetaStorageStubFactory {
    private final ManagedChannel managedChannel;

    public MetaStorageStubFactory(@Value("${meta-storage-host}") String host,
                                  @Value("${meta-storage-port}") int port) {
        managedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    }


    public MetaStorageServiceGrpc.MetaStorageServiceBlockingStub getMetaStorage() {
        return MetaStorageServiceGrpc.newBlockingStub(managedChannel);
    }
}
