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
 * This class Mocks an {@link AsyncAssembly}, which has an error during upload.
 */
public class MockUploadErrorAsyncAssembly extends AsyncAssembly {
    /**
     * Instantiates an {@link AsyncAssembly} object which always throws an error if a file upload attempt is undertaken.
     * @param transloadit The {@link Transloadit} client.
     * @param listener An {@link UploadProgressListener}
     */
    public MockUploadErrorAsyncAssembly(Transloadit transloadit, UploadProgressListener listener) {
        super(transloadit, listener);
        tusClient = new MockTusClient();
        assemblyId = "";
    }

    /**
     * Instantiates an {@link AsyncAssembly} object which always throws an error if a file upload attempt is undertaken.
     * @param transloadit The {@link Transloadit} client.
     * @param listener An {@link AssemblyProgressListener}
     */
    public MockUploadErrorAsyncAssembly(Transloadit transloadit, AssemblyProgressListener listener) {
        super(transloadit, listener);
        tusClient = new MockTusClient();
        assemblyId = "";
    }

    /**
     * Nested class which provides a mocked {@link TusUploader}, which always throws an exception if
     * {@link TusUploader#uploadChunk()} gets called.
     */
    class MockTusClient extends TusClient {
        /**
         * Instantiates a a mocked {@link TusUploader}, which always throws an exception if
         * {@link TusUploader#uploadChunk()} gets called.
         * @param upload {@link TusUpload}
         * @return {@link TusUploader}, which always throws an exception if {@link TusUploader#uploadChunk()} gets
         * called.
         * @throws ProtocolException if {@link TusUploader#uploadChunk()} gets called.
         * @throws IOException if an IOError occurs.
         */
        @Override
        public TusUploader resumeOrCreateUpload(@NotNull TusUpload upload) throws ProtocolException, IOException {
            TusUploader uploader = Mockito.mock(TusUploader.class);
            Mockito.when(uploader.uploadChunk()).thenThrow(new ProtocolException("some error message"));
            return uploader;
        }
    }
}
