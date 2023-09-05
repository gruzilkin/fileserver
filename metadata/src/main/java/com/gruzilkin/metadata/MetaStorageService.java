package com.gruzilkin.metadata;

import com.gruzilkin.common.FileSaveRequest;
import com.gruzilkin.common.FileSaveResponse;
import com.gruzilkin.common.MetaStorageServiceGrpc;
import com.gruzilkin.metadata.data.entity.Block;
import com.gruzilkin.metadata.data.entity.File;
import com.gruzilkin.metadata.data.repository.FileRepository;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MetaStorageService extends MetaStorageServiceGrpc.MetaStorageServiceImplBase {
    private final Logger log = LoggerFactory.getLogger(MetaStorageService.class);

    private final FileRepository fileRepository;

    public MetaStorageService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public void save(FileSaveRequest request, StreamObserver<FileSaveResponse> responseObserver) {
        var blockKeys = request.getBlockIdsList();

        log.info("Registering " + request.getFileName() + " with block IDs: " + String.join(", ", blockKeys));

        int sort = 1;
        File file = new File();
        List<Block> blocks = new ArrayList<>();
        for (var blockKey : blockKeys) {
            blocks.add(new Block(file, sort++, blockKey));
        }
        file.setBlocks(blocks);
        file = fileRepository.save(file);

        Span.current()
                .addEvent("saved file", Attributes.builder().put("file_id", file.getId())
                .build());

        var response = FileSaveResponse.newBuilder().setFileId(file.getId()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
