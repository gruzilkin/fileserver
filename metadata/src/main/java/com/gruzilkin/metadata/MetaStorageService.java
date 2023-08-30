package com.gruzilkin.metadata;

import com.gruzilkin.common.*;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MetaStorageService extends MetaStorageServiceGrpc.MetaStorageServiceImplBase {
    private final Logger log = LoggerFactory.getLogger(MetaStorageService.class);

    private long fileId = 1;

    @Override
    public void save(FileSaveRequest request, StreamObserver<FileSaveResponse> responseObserver) {
        var blocks = request.getBlockIdsList();

        log.info("Registering " + request.getFileName() + " with block IDs: " + String.join(", ", blocks));

        var response = FileSaveResponse.newBuilder().setFileId(fileId++).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
