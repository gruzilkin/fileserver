package com.gruzilkin.fileserver.blockstorage;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.rpc.Code;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import com.gruzilkin.fileserver.blockstorage.service.BlockStorageService;
import com.gruzilkin.fileserver.common.*;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BlockStorageServiceGrpcImpl extends BlockStorageServiceGrpc.BlockStorageServiceImplBase {
    private final Logger log = LoggerFactory.getLogger(BlockStorageServiceGrpcImpl.class);

    private final BlockStorageService blockStorageService;

    public BlockStorageServiceGrpcImpl(BlockStorageService blockStorageService) {
        this.blockStorageService = blockStorageService;
    }

    @Override
    public void save(BlockSaveRequest request, StreamObserver<BlockSaveResponse> responseObserver) {
        log.info("Received block of size " + request.getBlockContent().size());
        var block = blockStorageService.save(request.getBlockContent().toByteArray());

        var response = BlockSaveResponse.newBuilder()
                .setBlockId(block.id)
                .setHash(block.hash)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void read(BlockReadRequest request, StreamObserver<BlockReadResponse> observer) {
        var responseObserver = (ServerCallStreamObserver<BlockReadResponse>)observer;

        var hashes = request.getBlockHashList();

        for (var hash : hashes) {
            var data = blockStorageService.findById(hash);
            if (data == null) {
                Status status = Status.newBuilder()
                        .setCode(Code.NOT_FOUND.getNumber())
                        .setMessage("Block not found")
                        .addDetails(Any.pack(ErrorInfo.newBuilder()
                                .putMetadata("hash", hash)
                                .build()))
                        .build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                return;
            }

            while(!responseObserver.isReady()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Status status = Status.newBuilder()
                            .setCode(Code.ABORTED.getNumber())
                            .build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                    return;
                }
            }

            var response = BlockReadResponse.newBuilder()
                    .setBlockContent(ByteString.copyFrom(data))
                    .build();
            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    }
}
