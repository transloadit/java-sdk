package com.transloadit.sdk.async;

import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;

/**
 * This class Mocks an {@link AsyncAssembly}, which has an error during execution.
 */
public class MockStatusErrorAsyncAssembly extends AsyncAssembly {

    /**
     * Instantiates an {@link AsyncAssembly} object which always throws an error if {@link AsyncAssembly#watchStatus()}
     * is called.
     * @param transloadit The {@link Transloadit} client.
     * @param listener An {@link UploadProgressListener}
     */
    public MockStatusErrorAsyncAssembly(Transloadit transloadit, UploadProgressListener listener) {
        super(transloadit, listener);
        tusClient = new MockAsyncAssembly.MockTusClient();
    }

    /**
     * Instantiates an {@link AsyncAssembly} object which always throws an error if {@link AsyncAssembly#watchStatus()}
     * is called.
     * @param transloadit The {@link Transloadit} client.
     * @param listener An {@link AssemblyProgressListener}
     */
    public MockStatusErrorAsyncAssembly(Transloadit transloadit, AssemblyProgressListener listener) {
        super(transloadit, listener);
        tusClient = new MockAsyncAssembly.MockTusClient();
    }

    /**
     * Always throws an Exception if status gets observed.
     * @return only throws Exception
     * @throws RequestException always
     */
    @Override
    protected AssemblyResponse watchStatus() throws RequestException {
        throw new RequestException("some request exception");
    }
}
