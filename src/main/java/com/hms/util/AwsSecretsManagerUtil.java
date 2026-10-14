package com.hms.util;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import java.time.Duration;

/**
 * Utility class for retrieving secrets from AWS Secrets Manager.
 *
 * Cloud Readiness Fix (cr-java-0113 - Lack of Externalized Secrets):
 * This utility replaces hardcoded credentials, API keys, and sensitive
 * configuration values with centralized secret management via AWS Secrets
 * Manager. This enables:
 *   - Centralized secret lifecycle management
 *   - Automatic secret rotation
 *   - Audit logging of secret access
 *   - Elimination of credentials from source code and property files
 *
 * Cloud Readiness Fix (cr-java-0097 - Missing Connection Timeouts):
 * Adds explicit connection timeout, socket timeout, and API call timeout to
 * the AWS Secrets Manager SDK client to prevent indefinite hangs and resource
 * exhaustion in cloud environments with variable network latency.
 *
 * Usage example:
 *   String dbPassword = AwsSecretsManagerUtil.getSecret("hms/db/password");
 *   String dbUrl      = AwsSecretsManagerUtil.getSecret("hms/db/url");
 */
public class AwsSecretsManagerUtil {

    /**
     * AWS region read from the environment variable AWS_REGION.
     * Falls back to "us-east-1" if the variable is not set.
     */
    private static final String AWS_REGION =
            System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : "us-east-1";

    // ---------------------------------------------------------------------------
    // Timeout configuration (cr-java-0097 - Missing Connection Timeouts)
    // ---------------------------------------------------------------------------

    /**
     * Maximum time to wait for a TCP connection to be established with the
     * AWS Secrets Manager endpoint. Prevents indefinite blocking when the
     * endpoint is unreachable.
     */
    private static final Duration HTTP_CONNECTION_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Maximum time to wait for data to be received on an established socket.
     * Prevents threads from hanging when the service is slow or unresponsive.
     */
    private static final Duration HTTP_SOCKET_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Maximum total time allowed for a single API call (including retries).
     * Ensures the caller is never blocked longer than this duration regardless
     * of retry attempts or slow responses.
     */
    private static final Duration API_CALL_TIMEOUT = Duration.ofSeconds(15);

    /**
     * Maximum time allowed for a single API call attempt (excluding retries).
     */
    private static final Duration API_CALL_ATTEMPT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Retrieves a secret value from AWS Secrets Manager.
     *
     * <p>The underlying HTTP client is configured with explicit connection,
     * socket, and API-call timeouts (cr-java-0097) to prevent indefinite
     * hangs in cloud environments with variable network latency.
     *
     * @param secretName the name or ARN of the secret stored in AWS Secrets Manager
     * @return the secret string value
     * @throws RuntimeException if the secret cannot be retrieved
     */
    public static String getSecret(String secretName) {
        // Build Apache HTTP client with explicit connection and socket timeouts
        // (cr-java-0097 - Missing Connection Timeouts)
        ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder()
                .connectionTimeout(HTTP_CONNECTION_TIMEOUT)
                .socketTimeout(HTTP_SOCKET_TIMEOUT);

        // Build client-level override configuration with API call timeouts
        ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(API_CALL_TIMEOUT)
                .apiCallAttemptTimeout(API_CALL_ATTEMPT_TIMEOUT)
                .build();

        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(AWS_REGION))
                .httpClientBuilder(httpClientBuilder)
                .overrideConfiguration(overrideConfig)
                .build();

        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            GetSecretValueResponse response = client.getSecretValue(request);
            return response.secretString();

        } catch (SecretsManagerException e) {
            throw new RuntimeException(
                    "Failed to retrieve secret '" + secretName + "' from AWS Secrets Manager: " + e.awsErrorDetails().errorMessage(), e);
        } finally {
            client.close();
        }
    }
}
