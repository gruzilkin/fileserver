package com.gruzilkin.blockstorage.service.impl;

import com.gruzilkin.blockstorage.data.cassandra.Block;
import com.gruzilkin.blockstorage.data.cassandra.repository.BlockRepository;
import com.gruzilkin.blockstorage.service.BlockStorageService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class BlockStorageServiceImpl implements BlockStorageService {
    private final Tracer tracer = GlobalOpenTelemetry.getTracer(this.getClass().getName());

    private final BlockRepository blockRepository;

    private final Cache cache;

    private final ExecutorService executorService = Executors.newWorkStealingPool();

    protected ExecutorService getExecutor() {
        return Context.taskWrapping(executorService);
    }

    public BlockStorageServiceImpl(BlockRepository blockRepository, RedisCacheManager cacheManager) {
        this.blockRepository = blockRepository;
        this.cache = cacheManager.getCache(BlockStorageServiceImpl.class.getName());
    }

    @Override
    public byte[] findById(String id) {
        var data = cache.get(id, byte[].class);
        if (data == null) {
            var block = blockRepository.findById(id);
            if (block.isPresent()) {
                data = block.get().getContent().array();

                final var pinnedData = data;
                getExecutor().execute(() -> {
                    var span = tracer.spanBuilder("writing read-through cache").setSpanKind(SpanKind.CLIENT).startSpan();
                    try (var scope = span.makeCurrent()) {
                        cache.put(id, pinnedData);
                    } finally {
                        span.end();
                    }
                });
            }
        }
        return data;
    }

    @Override
    public String save(byte[] content) {
        var key = generateKey(content);

        Block newBlock = new Block();
        newBlock.setId(key);
        newBlock.setContent(ByteBuffer.wrap(content));
        newBlock = blockRepository.save(newBlock);

        final var pinnedData = newBlock.getContent().array();
        getExecutor().execute(() -> {
            var span = tracer.spanBuilder("writing write-through cache").setSpanKind(SpanKind.CLIENT).startSpan();
            try (var scope = span.makeCurrent()) {
                cache.put(key, pinnedData);
            } finally {
                span.end();
            }
        });

        return newBlock.getId();
    }

    private String generateKey(byte[] content) {
        var span = tracer.spanBuilder("block key calculation").setSpanKind(SpanKind.INTERNAL).startSpan();
        try (var scope = span.makeCurrent()) {
            return DigestUtils.sha512_256Hex(content);
        } finally {
            span.end();
        }
    }
}
