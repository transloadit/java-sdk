package com.transloadit.sdk;

import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * This class serves as a Mock to {@link Assembly}, which can be used to test
 * Assemblies with resumability enabled and provides a suitable {@link MockSocket}.
 */
public class MockTusAssembly extends Assembly {
    private Socket socket;
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
