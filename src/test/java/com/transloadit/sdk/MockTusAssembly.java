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
        emitted = new HashMap<String, Object>();
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

    /**
     * Provides a {@link MockSocket} to simulate a WebSocket.
     * @param url URL the {@link MockSocket} should mock.
     * @return {@link MockSocket}
     */

    Socket getSocket(String url) {
        if (socket == null) {
            socket = new MockSocket();
        }
        return socket;
    }

    /**
     * This class defines a MockSocket in order to simulate the behavior of a WebSocket.
     */
    private class MockSocket extends Socket {
        private Emitter fakeSocket;
        private boolean connected = false;

        MockSocket() {
            super(new Manager(), "", new Manager.Options());
            fakeSocket = new Emitter();
        }

        @Override
        public Socket on(String event, Listener fn) {
            fakeSocket.on(event, fn);
            return this;
        }

        @Override
        public Socket emit(final String event, final Object... args) {
            fakeSocket.emit(event, args);
            emitted.put(event, args);
            return this;
        }

        @Override
        public Socket connect() {
            connected = true;
            fakeSocket.emit(Socket.EVENT_CONNECT);
            return this;
        }

        @Override
        public boolean connected() {
            return connected;
        }

        @Override
        public Socket disconnect() {
            connected = false;
            fakeSocket.emit(Socket.EVENT_DISCONNECT);
            return this;
        }
    }
}
