package com.gruzilkin.fileserver.metadata;

import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import com.gruzilkin.fileserver.common.*;
import com.gruzilkin.fileserver.metadata.data.repository.FileRepository;
import com.gruzilkin.fileserver.metadata.data.entity.Block;
import com.gruzilkin.fileserver.metadata.data.entity.File;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class MetaStorageService extends MetaStorageServiceGrpc.MetaStorageServiceImplBase {
    private final Logger log = LoggerFactory.getLogger(MetaStorageService.class);

    private final FileRepository fileRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    public MetaStorageService(FileRepository fileRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.fileRepository = fileRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional("kafkaTransactionManager")
    @Override
    public void save(FileSaveRequest request, StreamObserver<FileSaveResponse> responseObserver) {
        var ids = request.getIdsList().stream().toList();
        log.info("Registering " + request.getFileName()
                + " with block IDs: " + String.join(", ", ids));

        int sort = 1;
        File file = new File();
        List<Block> blocks = new ArrayList<>();
        for (var id : ids) {
            blocks.add(new Block(id, file, sort++));
        }
        file.setBlocks(blocks);
        file = fileRepository.save(file);

        ids.forEach(id -> kafkaTemplate.send("block-created", id));

        Span.current()
                .addEvent("saved file", Attributes.builder().put("file_id", file.getId())
                .build());

        var response = FileSaveResponse.newBuilder().setFileId(file.getId()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void read(FileReadRequest request, StreamObserver<FileReadResponse> responseObserver) {
        var file = fileRepository.findById(request.getFileId());
        if (file.isEmpty()) {
            Status status = Status.newBuilder()
                    .setCode(Code.NOT_FOUND.getNumber())
                    .setMessage("BlockId not found")
                    .addDetails(Any.pack(ErrorInfo.newBuilder()
                            .putMetadata("fileId", String.valueOf(request.getFileId()))
                            .build()))
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
        else {
            var ids = file.get().getBlocks().stream().map(Block::getId).toList();
            var response = FileReadResponse.newBuilder().addAllIds(ids).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
