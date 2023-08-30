package com.gruzilkin.fileserver.web;

import com.google.protobuf.ByteString;
import com.gruzilkin.common.BlockSaveRequest;
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

@Controller
public class FileController {
    private final Logger log = LoggerFactory.getLogger(FileController.class);


    @Autowired
    BlockStorageStubFactory clientFactory;

    @RequestMapping(value = "/file", method = RequestMethod.POST)
    public @ResponseBody String upload(HttpServletRequest request) throws IOException {
        boolean isMultipart = JakartaServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {
            log.info("not multipart request");
            return "not multipart request";
        }

        var blockStorageClient = clientFactory.getBlockStorage();

        // Create a new file upload handler
        JakartaServletFileUpload upload = new JakartaServletFileUpload();
        upload.getItemIterator(request).forEachRemaining(item -> {
            String name = item.getFieldName();
            InputStream stream = item.getInputStream();
            if (item.isFormField()) {
                log.info("Form field " + name + " detected.");
            } else {
                int blockId = 1;
                var bytes = stream.readNBytes(1024 * 1024);
                while (bytes.length > 0) {
                    log.info("Read block " + blockId + " with size " + bytes.length);
                    bytes = stream.readNBytes(1 * 1024 * 1024);
                    blockId += 1;
                    var saveRequest = BlockSaveRequest.newBuilder()
                            .setBlockContent(ByteString.copyFrom(bytes))
                            .build();
                    var saveResponse = blockStorageClient.save(saveRequest);
                    log.info("Saved block " + saveResponse.getBlockId());
                }

                log.info("File field " + name + " with file name " + item.getName() + " detected.");
            }
        });

        return "method finished";
    }
}
