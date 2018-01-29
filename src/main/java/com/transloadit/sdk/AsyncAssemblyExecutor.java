package com.transloadit.sdk;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsyncAssemblyExecutor {
    private final ExecutorService service;
    private Runnable runnable;

    AsyncAssemblyExecutor(AsyncAssembly.AssemblyRunnable runnable) {
        this.runnable = runnable;
        runnable.setExecutor(this);
        service = Executors.newSingleThreadExecutor();
    }

    void execute() {
        service.execute(runnable);
    }

    void blockingClose() {
        // todo investigate how this functions
        service.shutdown();
        boolean terminated = false;
        // wait till shutdown is done
        while (!terminated) {
            try {
                terminated = service.awaitTermination(800, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void close() {
        service.shutdown();
    }
}

