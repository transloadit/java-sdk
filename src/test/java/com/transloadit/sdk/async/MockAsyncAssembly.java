package com.transloadit.sdk.async;

import com.transloadit.sdk.Transloadit;
import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;
import org.jetbrains.annotations.NotNull;

import org.mockito.Mockito;

import java.io.IOException;

/**
 * This class serves as a Mock to {@link AsyncAssembly}, which can be used in tests.
*/
public class MockAsyncAssembly extends AsyncAssembly {

    /**
     * Instantiates a new {@link com.transloadit.sdk.MockTusAssembly} object.
     * @param transloadit The {@link Transloadit} client.
     * @param listener An {@link UploadProgressListener}
     */
    public MockAsyncAssembly(Transloadit transloadit, UploadProgressListener listener) {
        super(transloadit, listener);
        tusClient = new MockTusClient();
    }

    /**
     * Instantiates a new {@link com.transloadit.sdk.MockTusAssembly} object.
     * @param transloadit The {@link Transloadit} client.
     * @param listener An {@link AssemblyProgressListener}
     */
    public MockAsyncAssembly(Transloadit transloadit, AssemblyProgressListener listener) {
        super(transloadit, listener);
        tusClient = new MockTusClient();
    }

    /**
     * This method provides functionality to Mock progress on AsyncAssembly execution.
     * @param state {@link State} represents states of Assembly execution
     */
    @Override
    synchronized void setState(State state) {
        super.setState(state);
        if (this.state == State.UPLOADING) {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    /**
     * This nested class provides a Mock for the {@link TusClient} used by {@link MockAsyncAssembly}.
     */
    static class MockTusClient extends TusClient {
        /**
         * This method returns a mocked {@link TusUploader} to simulate actual file uploads.
         * @param upload {@link TusUpload}, not null
         * @return a mocked {@link TusUploader}
         * @throws ProtocolException if the server sends a request that cannot be processed.
         * @throws IOException if source cannot be read or writing to the HTTP request fails.
         */
        @Override
        public TusUploader resumeOrCreateUpload(@NotNull TusUpload upload) throws ProtocolException, IOException {
            TusUploader uploader = Mockito.mock(TusUploader.class);
            // 1077 / 3 = 359 i.e size of the LICENSE file
            Mockito.when(uploader.uploadChunk()).thenReturn(359, 359, 359, 0, -1);
            return uploader;
        }
    }
}
