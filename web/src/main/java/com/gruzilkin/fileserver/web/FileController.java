package com.gruzilkin.fileserver.web;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.gruzilkin.common.*;
import com.gruzilkin.fileserver.web.model.FileUploadResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@RestController
public class FileController {
    private final Logger log = LoggerFactory.getLogger(FileController.class);

    private final BlockStorageStubFactory blockStorageClientFactory;

    private final MetaStorageStubFactory metaStorageClientFactory;

    public FileController(BlockStorageStubFactory blockStorageClientFactory, MetaStorageStubFactory metaStorageClientFactory) {
        this.blockStorageClientFactory = blockStorageClientFactory;
        this.metaStorageClientFactory = metaStorageClientFactory;
    }

    @GetMapping(value = "/file/{id}")
    public StreamingResponseBody get(@PathVariable(value="id") int id) {
        var fileReadRequest = FileReadRequest.newBuilder().setFileId(id).build();
        var blockIds = metaStorageClientFactory.getMetaStorage().read(fileReadRequest).getBlockIdsList();

        var blockStorageClient = blockStorageClientFactory.getBlockStoragAsync();

        return out -> {
            for (var blockId : blockIds) {
                try {
                    var request = BlockReadRequest.newBuilder().setBlockId(blockId).build();
                    var data = blockStorageClient.read(request).get().getBlockContent().toByteArray();
                    out.write(data);
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
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
