package com.transloadit.sdk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class serves as a Mock to {@link Assembly}, which can be used to test
 * Assemblies with resumability enabled.
 */
public class MockTusAssembly extends Assembly {
    public MockTusAssembly(Transloadit transloadit) {
        super(transloadit);
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
