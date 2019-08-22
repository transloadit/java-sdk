package com.transloadit.sdk.async;

import com.transloadit.sdk.Transloadit;
import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;

import java.io.IOException;

public class MockUploadErrorAsyncAssembly extends AsyncAssembly {
    public MockUploadErrorAsyncAssembly(Transloadit transloadit, UploadProgressListener listener) {
        super(transloadit, listener);
        tusClient = new MockTusClient();
    }

    public MockUploadErrorAsyncAssembly(Transloadit transloadit, AssemblyProgressListener listener) {
        super(transloadit, listener);
        tusClient = new MockTusClient();
    }

    class MockTusClient extends TusClient {
        @Override
        public TusUploader resumeOrCreateUpload(@NotNull TusUpload upload) throws ProtocolException, IOException {
            TusUploader uploader = Mockito.mock(TusUploader.class);
            Mockito.when(uploader.uploadChunk()).thenThrow(new ProtocolException("some error message"));
            return uploader;
        }
    }
}
