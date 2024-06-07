package com.gruzilkin.fileserver.web;

import com.google.protobuf.ByteString;
import com.gruzilkin.fileserver.common.*;
import com.gruzilkin.fileserver.web.model.FileUploadResponse;
import io.opentelemetry.api.GlobalOpenTelemetry;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
        var blockHashes = metaStorageClientFactory.getMetaStorage().read(fileReadRequest).getBlockHashesList();
        var blockStorageClient = blockStorageClientFactory.getBlockStorage();

        return out -> {
            BlockReadRequest request = BlockReadRequest.newBuilder().addAllHash(blockHashes).build();
            var response = blockStorageClient.read(request);

            response.forEachRemaining(block -> {
                try {
                    out.write(block.getContent().toByteArray());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        };
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
                            .setContent(ByteString.copyFrom(bytes))
                            .build();
                    var saveResponseFuture = blockStorageClient.save(saveRequest);
                    futures.add(saveResponseFuture);
                }

                var blockDescriptions = futures.stream().map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).map(blockSaveResponse -> BlockDescription.newBuilder()
                        .setBlockId(blockSaveResponse.getId())
                        .setHash(blockSaveResponse.getHash()).build())
                .toList();

                var fileSaveRequest = FileSaveRequest.newBuilder()
                        .addAllBlocks(blockDescriptions)
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
