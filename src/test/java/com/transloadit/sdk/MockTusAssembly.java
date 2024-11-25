package com.transloadit.sdk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * This class serves as a Mock to {@link Assembly}, which can be used to test.
 */
public class MockTusAssembly extends Assembly {
    /**
     * Stores emitted information.
     */
    Map<String, Object> emitted;

    /**
     * Constructs a new instance of {@link MockTusAssembly}.
     * @param transloadit The {@link Transloadit} client.
     */
    public MockTusAssembly(Transloadit transloadit) {
        super(transloadit);
        assemblyId = "";
    }

    @Override
    protected void processTusFile(File file, String fieldName, String assemblyUrl) throws IOException {
        // do nothing
    }

    @Override
    protected void processTusFile(InputStream inputStream, String fieldName, String assemblyUrl) throws IOException {
        // do nothing
    }
}
