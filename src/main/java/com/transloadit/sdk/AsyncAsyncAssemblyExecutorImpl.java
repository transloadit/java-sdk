package com.transloadit.sdk;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsyncAsyncAssemblyExecutorImpl implements AsyncAssemblyExecutor {
    private final ExecutorService service;
    private Runnable runnable;

    AsyncAsyncAssemblyExecutorImpl(AsyncAssembly.AssemblyRunnable runnable) {
        this.runnable = runnable;
        runnable.setExecutor(this);
        service = Executors.newSingleThreadExecutor();
    }

    @Override
    public void execute() {
        service.execute(runnable);
    }

    @Override
    public void hardStop() {
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

    @Override
    public void stop() {
        service.shutdown();
    }
}

