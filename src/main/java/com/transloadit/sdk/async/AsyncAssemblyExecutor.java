package com.transloadit.sdk.async;

public interface AsyncAssemblyExecutor {
    /**
     * starts the execution of the assembly on a separate thread.
     */
    void execute();

    /**
     * A blocking method that stops the execution of the assembly.
     * This method will wait till the execution is stopped.
     */
    void hardStop();

    /**
     * A non-blocking method that stops the execution of the assembly.
     */
    void stop();
}
