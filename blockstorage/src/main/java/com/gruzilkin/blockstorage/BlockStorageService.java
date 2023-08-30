package com.gruzilkin.blockstorage;

import com.gruzilkin.blockstorage.data.cassandra.Block;
import com.gruzilkin.common.BlockStorageServiceGrpc;
import com.gruzilkin.common.SaveRequest;
import com.gruzilkin.common.SaveResponse;
import com.gruzilkin.blockstorage.data.cassandra.repository.BlockRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BlockStorageService extends BlockStorageServiceGrpc.BlockStorageServiceImplBase {
    private final Logger log = LoggerFactory.getLogger(BlockStorageService.class);

    private final BlockRepository blockRepository;

    public BlockStorageService(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    @Override
    public void save(SaveRequest request, StreamObserver<SaveResponse> responseObserver) {
        log.info("Received block of size " + request.getBlockContent().size());

        Block newBlock = new Block();
        newBlock.setId(UUID.randomUUID());
        newBlock.setContent(request.getBlockContent().asReadOnlyByteBuffer());
        blockRepository.save(newBlock);

        var response = SaveResponse.newBuilder()
                .setBlockId(newBlock.getId().toString())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
