package org.hobbit.utils.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class KubeHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubeHelper.class);
    private static final String DEFAULT_IP = "unknown";
    private static final String DOMAIN_SUFFIX = ".pod.cluster.local";

    /**
     * Gets a DNS-friendly IP for the pod.
     *
     * @return DNS-friendly hostname (or IP address if not available).
     */
    public static String getDnsFriendlyIP() {
        String namespace = getEnvVariable("POD_NAMESPACE");
        if(namespace == null || "".equals(namespace)) {
            LOGGER.warn("No pod namespace found in environment variable POD_NAMESPACE use default");
            namespace = "default";
        }
        String ip = getPodIP();

        if (!DEFAULT_IP.equals(ip)) {
            String valueToReturn = formatDnsFriendlyIp(ip, namespace);
            LOGGER.info("dns friendly version for {} in namespace {} is {}", ip, namespace, valueToReturn);
            return valueToReturn;
        } else {
            LOGGER.error("Error getting pod IP: {} in namespace: {}", ip, namespace);
            throw new IllegalStateException("Error getting pod IP: " + ip + " in namespace: " + namespace);
        }
    }

    /**
     * Retrieves the pod's IP address.
     *
     * @return The IP address of the pod, or "unknown" if retrieval fails.
     */
    public static String getPodIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            LOGGER.warn("Failed to retrieve pod IP, returning default value: {}", DEFAULT_IP, e);
            return DEFAULT_IP;
        }
    }

    /**
     * Retrieves an environment variable, ensuring a non-null value.
     *
     * @param key The environment variable key.
     * @return The value of the environment variable or an empty string if not found.
     */
    protected static String getEnvVariable(String key) {
        return Optional.ofNullable(System.getenv(key)).orElse("").trim();
    }

    /**
     * Formats the IP into a DNS-friendly format for Kubernetes.
     *
     * @param ip        The pod's IP address.
     * @param namespace The namespace in which the pod is running.
     * @return A DNS-friendly hostname.
     */
    private static String formatDnsFriendlyIp(String ip, String namespace) {
        return String.format("%s.%s%s", ip.replace(".", "-"), namespace, DOMAIN_SUFFIX);
    }
}
