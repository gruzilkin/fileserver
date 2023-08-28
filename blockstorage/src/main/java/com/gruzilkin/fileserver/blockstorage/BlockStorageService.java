package com.gruzilkin.fileserver.blockstorage;

import com.gruzilkin.common.BlockStorageServiceGrpc;
import com.gruzilkin.common.SaveRequest;
import com.gruzilkin.common.SaveResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BlockStorageService extends BlockStorageServiceGrpc.BlockStorageServiceImplBase {
    private final Logger log = LoggerFactory.getLogger(BlockStorageService.class);

    @Override
    public void save(SaveRequest request, StreamObserver<SaveResponse> responseObserver) {
        log.info("Received block of size " + request.getBlockContent().size());
        var response = SaveResponse.newBuilder()
                .setBlockId(UUID.randomUUID().toString())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
