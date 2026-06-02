package com.transloadit.sdk;

import com.transloadit.sdk.response.AssemblyResponse;

/**
 * Result returned after uploading one file through a TUS Assembly.
 */
public class UploadTusAssemblyResult {
    private final AssemblyResponse assembly;
    private final String uploadUrl;

    public UploadTusAssemblyResult(AssemblyResponse assembly, String uploadUrl) {
        this.assembly = assembly;
        this.uploadUrl = uploadUrl;
    }

    public AssemblyResponse getAssembly() {
        return assembly;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }
}
