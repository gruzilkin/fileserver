package com.gruzilkin.fileserver.web.model;

public class FileUploadResponse {
    private final long fileId;

    public FileUploadResponse(long fileId) {
        this.fileId = fileId;
    }

    public long getFileId() {
        return fileId;
    }
}
