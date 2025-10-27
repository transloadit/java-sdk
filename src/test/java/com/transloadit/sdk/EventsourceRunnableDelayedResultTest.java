package com.transloadit.sdk;

import com.launchdarkly.eventsource.ConnectStrategy;
import com.launchdarkly.eventsource.ErrorStrategy;
import com.launchdarkly.eventsource.RetryDelayStrategy;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.transloadit.sdk.response.AssemblyResponse;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class EventsourceRunnableDelayedResultTest {

    private HttpServer server;
    private CountDownLatch requestLatch;

    @BeforeEach
    void setUp() throws IOException {
        requestLatch = new CountDownLatch(1);
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/sse", new DelayedResultHandler(requestLatch));
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void drainsResultEmittedAfterAssemblyFinished() throws Exception {
        String sseUrl = "http://localhost:" + server.getAddress().getPort() + "/sse";
        String sslUrl = "https://example.com/assemblies/123";

        Transloadit transloadit = Mockito.mock(Transloadit.class);
        AssemblyResponse initialResponse = Mockito.mock(AssemblyResponse.class);
        JSONObject initialJson = new JSONObject().put("ok", "ASSEMBLY_UPLOADING");
        when(initialResponse.getSslUrl()).thenReturn(sslUrl);
        when(initialResponse.json()).thenReturn(initialJson);

        AssemblyResponse finishedResponse = Mockito.mock(AssemblyResponse.class);
        JSONObject finishedJson = new JSONObject().put("ok", "ASSEMBLY_COMPLETED");
        when(finishedResponse.json()).thenReturn(finishedJson);
        when(transloadit.getAssemblyByUrl(anyString())).thenReturn(finishedResponse);

        CountDownLatch finishedLatch = new CountDownLatch(1);
        CountDownLatch resultLatch = new CountDownLatch(1);
        AtomicReference<String> resultStep = new AtomicReference<>();

        AssemblyListener listener = new AssemblyListener() {
            @Override
            public void onAssemblyFinished(AssemblyResponse response) {
                finishedLatch.countDown();
            }

            @Override
            public void onError(Exception error) {
                throw new AssertionError("Unexpected SSE error", error);
            }

            @Override public void onMetadataExtracted() { }
            @Override public void onAssemblyUploadFinished() { }
            @Override public void onFileUploadFinished(JSONObject uploadInformation) { }
            @Override public void onFileUploadPaused(String name) { }
            @Override public void onFileUploadResumed(String name) { }
            @Override public void onFileUploadProgress(long uploadedBytes, long totalBytes) { }
            @Override public void onAssemblyProgress(JSONObject progressPerOriginalFile) { }

            @Override
            public void onAssemblyResultFinished(org.json.JSONArray result) {
                resultStep.compareAndSet(null, result.optString(0));
                resultLatch.countDown();
            }
        };

        ConnectStrategy connectStrategy = ConnectStrategy.http(URI.create(sseUrl));
        RetryDelayStrategy retryStrategy = RetryDelayStrategy.defaultStrategy();
        ErrorStrategy errorStrategy = ErrorStrategy.alwaysContinue();

        EventsourceRunnable runnable = new EventsourceRunnable(
                transloadit,
                initialResponse,
                listener,
                connectStrategy,
                retryStrategy,
                errorStrategy,
                false
        );

        Thread thread = new Thread(runnable, "sse-delayed-result-test");
        thread.start();

        assertTrue(requestLatch.await(5, TimeUnit.SECONDS), "SSE server not contacted");
        assertTrue(finishedLatch.await(5, TimeUnit.SECONDS), "assembly_finished not received");
        assertTrue(resultLatch.await(15, TimeUnit.SECONDS), "Delayed result not received");
        assertEquals("resize", resultStep.get(), "Unexpected step name");

        thread.join(TimeUnit.SECONDS.toMillis(5));
    }

    private static final class DelayedResultHandler implements HttpHandler {
        private final CountDownLatch latch;

        private DelayedResultHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            latch.countDown();
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                write(os, "event: message\n");
                write(os, "data: assembly_finished\n\n");
                sleep(3000);
                write(os, "event: assembly_result_finished\n");
                write(os, "data: [\"resize\",{\"id\":\"abc\"}]\n\n");
            } finally {
                exchange.close();
            }
        }

        private static void write(OutputStream os, String value) throws IOException {
            os.write(value.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        private static void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
