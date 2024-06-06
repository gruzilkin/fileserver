package com.gruzilkin.fileserver.blockstorage.service.impl;

import com.gruzilkin.fileserver.blockstorage.data.cassandra.ColdBlock;
import com.gruzilkin.fileserver.blockstorage.data.cassandra.HashToId;
import com.gruzilkin.fileserver.blockstorage.data.cassandra.HashToIdKey;
import com.gruzilkin.fileserver.blockstorage.data.cassandra.repository.ColdBlockRepository;
import com.gruzilkin.fileserver.blockstorage.data.cassandra.repository.HashToIdRepository;
import com.gruzilkin.fileserver.blockstorage.data.cassandra.repository.HotBlockRepository;
import com.gruzilkin.fileserver.blockstorage.service.BlockStorageService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class BlockStorageServiceImpl implements BlockStorageService {
    private final Tracer tracer = GlobalOpenTelemetry.getTracer(this.getClass().getName());

    private final HotBlockRepository hotBlockRepository;

    private final ColdBlockRepository coldBlockRepository;

    private final HashToIdRepository hashToIdRepository;

    private final Cache cache;

    private final ExecutorService executorService = Executors.newWorkStealingPool();

    protected ExecutorService getExecutor() {
        return Context.taskWrapping(executorService);
    }

    public BlockStorageServiceImpl(HotBlockRepository hotBlockRepository, ColdBlockRepository coldBlockRepository, HashToIdRepository hashToIdRepository, RedisCacheManager cacheManager) {
        this.hotBlockRepository = hotBlockRepository;
        this.coldBlockRepository = coldBlockRepository;
        this.hashToIdRepository = hashToIdRepository;
        this.cache = cacheManager.getCache(BlockStorageServiceImpl.class.getName());
    }

    @Override
    public byte[] findById(String id) {
        var readBlockSpan = tracer.spanBuilder("block read").setSpanKind(SpanKind.INTERNAL).startSpan();
        try (var readBlockScope = readBlockSpan.makeCurrent()) {
            var data = cache.get(id, byte[].class);
            if (data == null) {
                var block = coldBlockRepository.findById(id);
                if (block.isPresent()) {
                    data = block.get().getContent().array();

                    final var pinnedData = data;
                    getExecutor().execute(() -> {
                        var span = tracer.spanBuilder("writing read-through cache").setSpanKind(SpanKind.CLIENT).startSpan();
                        try (var scope = span.makeCurrent()) {
                            cache.putIfAbsent(id, pinnedData);
                        } finally {
                            span.end();
                        }
                    });
                }
            }
            return data;
        } finally {
            readBlockSpan.end();
        }
    }

    @Override
    public BlockDescription save(final byte[] content) {
        var writeBlockSpan = tracer.spanBuilder("block write").setSpanKind(SpanKind.INTERNAL).startSpan();
        try (var writeBlockScope = writeBlockSpan.makeCurrent()) {
            var key = UUID.randomUUID().toString();
            var hash = calculateHash(content);
            hotBlockRepository.saveWithTtl(key, ByteBuffer.wrap(content), hash, Instant.now(), 10);
            return new BlockDescription(key, hash);
        } finally {
            writeBlockSpan.end();
        }
    }

    @Override
    public void commit(String id) {
        var writeCommitSpan = tracer.spanBuilder("block write").setSpanKind(SpanKind.INTERNAL).startSpan();
        try (var writeBlockScope = writeCommitSpan.makeCurrent()) {
            var hotBlock = hotBlockRepository.findById(id).get();

            var coldBlock = new ColdBlock();
            coldBlock.setHash(hotBlock.getHash());
            coldBlock.setContent(hotBlock.getContent());
            coldBlock.setUpdateDate(Instant.now());
            coldBlockRepository.save(coldBlock);

            var hashToIdKey = new HashToIdKey();
            hashToIdKey.setHash(hotBlock.getHash());
            hashToIdKey.setId(hotBlock.getId());
            var hashToId = new HashToId();
            hashToId.setKey(hashToIdKey);
            hashToIdRepository.save(hashToId);

            hotBlockRepository.deleteById(id);
        } finally {
            writeCommitSpan.end();
        }
    }

    private String calculateHash(byte[] content) {
        var span = tracer.spanBuilder("block key calculation").setSpanKind(SpanKind.INTERNAL).startSpan();
        try (var scope = span.makeCurrent()) {
            return DigestUtils.sha512_256Hex(content);
        } finally {
            span.end();
        }
    }
}
