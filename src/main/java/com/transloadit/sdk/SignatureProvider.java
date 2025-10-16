package com.transloadit.sdk;

/**
 * Interface for providing external signatures for Transloadit requests.
 * Implement this interface to generate signatures on your backend server
 * instead of including the secret key in your application.
 *
 * <p>This approach significantly improves security by keeping your secret key
 * on your backend server, preventing it from being exposed in client applications.</p>
 *
 * <p>Example implementation:</p>
 * <pre>{@code
 * SignatureProvider provider = new SignatureProvider() {
 *     @Override
 *     public String generateSignature(String paramsJson) throws Exception {
 *         // Make a synchronous request to your backend
 *         HttpResponse response = httpClient.post("/api/sign")
 *             .body(paramsJson)
 *             .execute();
 *
 *         if (response.isSuccessful()) {
 *             return response.body().getString("signature");
 *         } else {
 *             throw new Exception("Failed to generate signature: " + response.statusCode());
 *         }
 *     }
 * };
 * }
 * }</code></pre>
 *
 * <p>For asynchronous implementations, consider using CompletableFuture or similar patterns
 * to bridge async operations to this synchronous interface.</p>
 *
 * @see <a href="https://transloadit.com/docs/api/authentication/">Transloadit Authentication Documentation</a>
 * @since 2.1.0
 */
public interface SignatureProvider {

    /**
     * Generate a signature for the given parameters JSON string.
     *
     * <p>The implementation should generate a signature for the provided JSON parameters
     * according to Transloadit's authentication requirements, typically using HMAC-SHA384
     * with your secret key.</p>
     *
     * <p>This method is called synchronously, so implementations should either be fast
     * or use appropriate timeout mechanisms. For network-based implementations, consider
     * caching signatures when appropriate.</p>
     *
     * @param paramsJson The JSON string containing the request parameters to sign
     * @return The generated signature string (should include the algorithm prefix, e.g., "sha384:...")
     * @throws Exception if signature generation fails for any reason
     */
    String generateSignature(String paramsJson) throws Exception;
}
