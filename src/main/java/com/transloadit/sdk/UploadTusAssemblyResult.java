package com.transloadit.sdk;

import com.transloadit.sdk.response.AssemblyResponse;

// This file is generated from Transloadit API2 contracts. If it looks wrong,
// please report the issue instead of editing this file by hand; the source fix
// belongs in the contract generator so all SDKs stay in sync.

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
