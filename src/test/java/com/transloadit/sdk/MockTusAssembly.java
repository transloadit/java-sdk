package com.transloadit.sdk;

import io.tus.java.client.ProtocolException;

import java.io.File;
import java.io.IOException;

/**
 * This class serves as a Mock to {@link Assembly}, which can be used to test
 * Assemblies with resumability enabled.
 */
public class MockTusAssembly extends Assembly {
    public MockTusAssembly(Transloadit transloadit) {
        super(transloadit);
    }

    protected void processTusFile(File file, String fieldName, String assemblyUrl) throws IOException, ProtocolException {
        // do nothing
    }
}
