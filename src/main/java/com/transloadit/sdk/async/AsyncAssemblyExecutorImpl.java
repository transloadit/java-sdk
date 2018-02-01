package com.transloadit.sdk.async;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class AsyncAssemblyExecutorImpl implements AsyncAssemblyExecutor {
    private final ExecutorService service;
    private Runnable runnable;

    AsyncAssemblyExecutorImpl(AsyncAssembly.AssemblyRunnable runnable) {
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

