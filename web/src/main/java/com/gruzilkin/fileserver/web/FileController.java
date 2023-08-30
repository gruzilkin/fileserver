package com.gruzilkin.fileserver.web;

import com.google.protobuf.ByteString;
import com.gruzilkin.common.BlockSaveRequest;
import com.gruzilkin.common.FileSaveRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

@Controller
public class FileController {
    private final Logger log = LoggerFactory.getLogger(FileController.class);

    private final BlockStorageStubFactory blockStorageClientFactory;

    private final MetaStorageStubFactory metaStorageClientFactory;

    public FileController(BlockStorageStubFactory blockStorageClientFactory, MetaStorageStubFactory metaStorageClientFactory) {
        this.blockStorageClientFactory = blockStorageClientFactory;
        this.metaStorageClientFactory = metaStorageClientFactory;
    }

    @RequestMapping(value = "/file", method = RequestMethod.POST)
    public @ResponseBody String upload(HttpServletRequest request) throws IOException {
        boolean isMultipart = JakartaServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {
            log.info("not multipart request");
            return "not multipart request";
        }

        var blockStorageClient = blockStorageClientFactory.getBlockStorage();

        // Create a new file upload handler
        JakartaServletFileUpload upload = new JakartaServletFileUpload();
        upload.getItemIterator(request).forEachRemaining(item -> {
            String name = item.getFieldName();
            InputStream stream = item.getInputStream();
            if (item.isFormField()) {
                log.info("Form field " + name + " detected.");
            } else {
                var blockIds = new ArrayList<String>();
                while (true) {
                    var bytes = stream.readNBytes(1 * 1024 * 1024);
                    if (bytes.length == 0) {
                        break;
                    }
                    var saveRequest = BlockSaveRequest.newBuilder()
                            .setBlockContent(ByteString.copyFrom(bytes))
                            .build();
                    var saveResponse = blockStorageClient.save(saveRequest);
                    blockIds.add(saveResponse.getBlockId());
                    log.info("Saved block " + saveResponse.getBlockId());
                }

                var fileSaveRequest = FileSaveRequest.newBuilder()
                        .addAllBlockIds(blockIds)
                        .setFileName(item.getName())
                        .build();
                var fileSaveResponse = metaStorageClientFactory.getMetaStorage().save(fileSaveRequest);

                log.info("Saved file with ID " + fileSaveResponse.getFileId());
            }
        });

        return "method finished";
    }
}
