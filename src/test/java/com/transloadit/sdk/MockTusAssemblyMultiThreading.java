package com.transloadit.sdk;

import io.tus.java.client.TusUpload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MockTusAssemblyMultiThreading extends MockTusAssembly {
    /**
     * Constructs a new instance of {@link MockTusAssembly}.
     *
     * @param transloadit The {@link Transloadit} client.
     */
    public MockTusAssemblyMultiThreading(Transloadit transloadit) {
        super(transloadit);
    }

    /**
     * Processes a tus upload for multi threading.
     * @param file
     * @param fieldName
     * @param assemblyUrl
     * @throws IOException
     */
    @Override
    protected void processTusFile(File file, String fieldName, String assemblyUrl) throws IOException {
        TusUpload upload = getTusUploadInstance(file);

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("filename", file.getName());
        metadata.put("assembly_url", assemblyUrl);
        metadata.put("fieldname", fieldName);

        upload.setMetadata(metadata);

        uploads.add(upload);
    }

    /**
     * Processes a file input stream.
     * @param inputStream
     * @param fieldName
     * @param assemblyUrl
     * @throws IOException
     */
    @Override
    protected void processTusFile(InputStream inputStream, String fieldName, String assemblyUrl) throws IOException {
        TusUpload upload = getTusUploadInstance(inputStream, fieldName, assemblyUrl);

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("filename", fieldName);
        metadata.put("assembly_url", assemblyUrl);
        metadata.put("fieldname", fieldName);

        upload.setMetadata(metadata);

        uploads.add(upload);
    }
}
