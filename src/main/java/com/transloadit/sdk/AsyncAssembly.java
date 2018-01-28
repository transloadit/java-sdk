package com.transloadit.sdk;

public class AsyncAssembly extends Assembly {
    private AssemblyProgressListener listener;

    public AsyncAssembly(Transloadit transloadit, AssemblyProgressListener listener) {
        super(transloadit);
        this.listener = listener;
    }
}
