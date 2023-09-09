package com.gruzilkin.fileserver.web;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.gruzilkin.common.*;
import com.gruzilkin.fileserver.web.model.FileUploadResponse;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@RestController
public class FileController {
    private final Logger log = LoggerFactory.getLogger(FileController.class);

    private final BlockStorageStubFactory blockStorageClientFactory;

    private final MetaStorageStubFactory metaStorageClientFactory;

    private final ExecutorService executorService = Executors.newWorkStealingPool();

    private final Tracer tracer = GlobalOpenTelemetry.getTracer(getClass().getName());

    public FileController(BlockStorageStubFactory blockStorageClientFactory, MetaStorageStubFactory metaStorageClientFactory) {
        this.blockStorageClientFactory = blockStorageClientFactory;
        this.metaStorageClientFactory = metaStorageClientFactory;
    }

    protected ExecutorService getExecutor() {
        return Context.taskWrapping(executorService);
    }

    @GetMapping(value = "/file/{id}")
    public StreamingResponseBody get(@PathVariable(value="id") int id) {
        var fileReadRequest = FileReadRequest.newBuilder().setFileId(id).build();
        var blockIds = metaStorageClientFactory.getMetaStorage().read(fileReadRequest).getBlockIdsList();

        var responses = downloader(blockIds);
        return out -> {
            while (true) {
                try {
                    var response = responses.take();
                    if (response == QUEUE_FINISHED) {
                        break;
                    }
                    else {
                        var future = (ListenableFuture<BlockReadResponse>) response;
                        var data = future.get().getBlockContent().toByteArray();
                        var span = tracer.spanBuilder("write to response stream").setSpanKind(SpanKind.INTERNAL).startSpan();
                        try (var scope = span.makeCurrent()) {
                            out.write(data);
                        } finally {
                            span.end();
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private static final Object QUEUE_FINISHED = new Object();
    protected BlockingQueue<Object> downloader(List<String> blockIds) {
        BlockingQueue<Object> result = new LinkedBlockingQueue<>(1);

        getExecutor().execute(() -> {
            var span = tracer.spanBuilder("downloader").setSpanKind(SpanKind.INTERNAL).startSpan();
            try (var scope = span.makeCurrent()) {
                var blockStorageClient = blockStorageClientFactory.getBlockStoragAsync();

                for (var blockId : blockIds) {
                    var request = BlockReadRequest.newBuilder().setBlockId(blockId).build();
                    var future = blockStorageClient.read(request);
                    result.put(future);
                }
                result.put(QUEUE_FINISHED);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                span.end();
            }
        });

        return result;
    }

    @PostMapping(value = "/file")
    public FileUploadResponse upload(HttpServletRequest request) throws Exception {
        boolean isMultipart = JakartaServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {
            log.info("not multipart request");
            throw new ResponseStatusException(HttpStatusCode.valueOf(400));
        }

        var blockStorageClient = blockStorageClientFactory.getBlockStoragAsync();

        AtomicReference<FileUploadResponse> response = new AtomicReference<>();

        // Create a new file upload handler
        JakartaServletFileUpload upload = new JakartaServletFileUpload();
        upload.getItemIterator(request).forEachRemaining(item -> {
            String name = item.getFieldName();
            InputStream stream = item.getInputStream();
            if (item.isFormField()) {
                log.info("Form field " + name + " detected.");
            } else {
                var futures = new ArrayList<Future<BlockSaveResponse>>();
                while (true) {
                    var bytes = stream.readNBytes(1 * 1024 * 1024);
                    if (bytes.length == 0) {
                        break;
                    }
                    var saveRequest = BlockSaveRequest.newBuilder()
                            .setBlockContent(ByteString.copyFrom(bytes))
                            .build();
                    var saveResponseFuture = blockStorageClient.save(saveRequest);
                    futures.add(saveResponseFuture);
                }

                var blockIds = new ArrayList<String>();
                for (var future : futures) {
                    try {
                        var saveResponse = future.get();
                        blockIds.add(saveResponse.getBlockId());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }

                var fileSaveRequest = FileSaveRequest.newBuilder()
                        .addAllBlockIds(blockIds)
                        .setFileName(item.getName())
                        .build();
                var fileSaveResponse = metaStorageClientFactory.getMetaStorage().save(fileSaveRequest);

                log.info("Saved file with ID " + fileSaveResponse.getFileId());
                response.set(new FileUploadResponse(fileSaveResponse.getFileId()));
            }
        });

        if (response.get() == null) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400));
        }

        return response.get();
    }
}
