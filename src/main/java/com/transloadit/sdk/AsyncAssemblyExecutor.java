package com.transloadit.sdk;

public interface AsyncAssemblyExecutor {
    void execute();
    void hardStop();
    void stop();
}
