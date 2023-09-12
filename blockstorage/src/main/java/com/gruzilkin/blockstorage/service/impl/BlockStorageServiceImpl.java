package com.gruzilkin.blockstorage.service.impl;

import com.gruzilkin.blockstorage.data.cassandra.Block;
import com.gruzilkin.blockstorage.data.cassandra.repository.BlockRepository;
import com.gruzilkin.blockstorage.service.BlockStorageService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.time.Instant;
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
        var readBlockSpan = tracer.spanBuilder("block read").setSpanKind(SpanKind.INTERNAL).startSpan();
        try (var readBlockScope = readBlockSpan.makeCurrent()) {
            var data = cache.get(id, byte[].class);
            if (data == null) {
                var block = blockRepository.findById(id);
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
    public String save(final byte[] content) {
        var writeBlockSpan = tracer.spanBuilder("block write").setSpanKind(SpanKind.INTERNAL).startSpan();
        try (var writeBlockScope = writeBlockSpan.makeCurrent()) {
            var key = generateKey(content);

            if (blockRepository.existsById(key)) {
                Span.current().addEvent("Block already exists", Attributes.of(AttributeKey.stringKey("key"), key));
                return key;
            }

            getExecutor().execute(() -> {
                var span = tracer.spanBuilder("writing write-through cache").setSpanKind(SpanKind.CLIENT).startSpan();
                try (var scope = span.makeCurrent()) {
                    cache.put(key, content);
                } finally {
                    span.end();
                }
            });

            var block = new Block();
            block.setId(key);
            block.setContent(ByteBuffer.wrap(content));
            block.setUpdateDate(Instant.now());
            blockRepository.save(block);

            return key;
        } finally {
            writeBlockSpan.end();
        }
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
