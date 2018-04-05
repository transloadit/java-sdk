package com.transloadit.sdk.async;

import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;

public class MockStatusErrorAsyncAssembly extends AsyncAssembly {
    public MockStatusErrorAsyncAssembly(Transloadit transloadit, AssemblyProgressListener listener) {
        super(transloadit, listener);
        tusClient = new MockAsyncAssembly.MockTusClient();
    }

    @Override
    protected AssemblyResponse watchStatus() throws RequestException {
        throw new RequestException("some request exception");
    }
}
