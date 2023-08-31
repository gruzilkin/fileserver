package com.gruzilkin.blockstorage;

import com.gruzilkin.blockstorage.data.cassandra.Block;
import com.gruzilkin.common.BlockStorageServiceGrpc;
import com.gruzilkin.common.BlockSaveRequest;
import com.gruzilkin.common.BlockSaveResponse;
import com.gruzilkin.blockstorage.data.cassandra.repository.BlockRepository;
import io.grpc.stub.StreamObserver;
import org.apache.commons.codec.digest.DigestUtils;
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
    public void save(BlockSaveRequest request, StreamObserver<BlockSaveResponse> responseObserver) {
        log.info("Received block of size " + request.getBlockContent().size());

        var key = DigestUtils.sha512_256Hex(request.getBlockContent().toByteArray());
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
}
