package com.transloadit.sdk.async;

import com.transloadit.sdk.Transloadit;
import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;
import org.jetbrains.annotations.NotNull;

import org.mockito.Mockito;

import java.io.IOException;

public class MockAsyncAssembly extends AsyncAssembly {
    public MockAsyncAssembly(Transloadit transloadit, AssemblyProgressListener listener) {
        super(transloadit, listener);
        tusClient = new MockTusClient();
    }

    static class MockTusClient extends TusClient {
        @Override
        public TusUploader resumeOrCreateUpload(@NotNull TusUpload upload) throws ProtocolException, IOException {
            TusUploader uploader = Mockito.mock(TusUploader.class);
            // 1077 / 3 = 359 i.e size of the LICENSE file
            Mockito.when(uploader.uploadChunk()).thenReturn(359,359, 359, 0, -1);
            return uploader;
        }
    }
}
