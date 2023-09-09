package com.gruzilkin.blockstorage;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.rpc.Code;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import com.gruzilkin.blockstorage.data.cassandra.Block;
import com.gruzilkin.blockstorage.data.cassandra.repository.BlockRepository;
import com.gruzilkin.common.*;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BlockStorageService extends BlockStorageServiceGrpc.BlockStorageServiceImplBase {
    private final Logger log = LoggerFactory.getLogger(BlockStorageService.class);

    private final Tracer tracer = GlobalOpenTelemetry.getTracer(getClass().getName());

    private final BlockRepository blockRepository;

    public BlockStorageService(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    @Override
    public void save(BlockSaveRequest request, StreamObserver<BlockSaveResponse> responseObserver) {
        log.info("Received block of size " + request.getBlockContent().size());

        var key = generateKey(request);
        var content = request.getBlockContent().asReadOnlyByteBuffer();

        Block newBlock = new Block();
        newBlock.setId(key);
        newBlock.setContent(content);
        blockRepository.save(newBlock);

        var response = BlockSaveResponse.newBuilder()
                .setBlockId(newBlock.getId())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private String generateKey(BlockSaveRequest request) {
        var span = tracer.spanBuilder("block key calculation").setSpanKind(SpanKind.INTERNAL).startSpan();
        try (var scope = span.makeCurrent()) {
            return DigestUtils.sha512_256Hex(request.getBlockContent().toByteArray());
        } finally {
            span.end();
        }
    }

    @Override
    public void read(BlockReadRequest request, StreamObserver<BlockReadResponse> responseObserver) {
        var key = request.getBlockId();
        var block = blockRepository.findById(key);

        if (block.isEmpty()) {
            Status status = Status.newBuilder()
                    .setCode(Code.NOT_FOUND.getNumber())
                    .setMessage("BlockId not found")
                    .addDetails(Any.pack(ErrorInfo.newBuilder()
                            .putMetadata("blockId", key)
                            .build()))
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
        else {
            var response = BlockReadResponse.newBuilder()
                    .setBlockContent(ByteString.copyFrom(block.get().getContent()))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
