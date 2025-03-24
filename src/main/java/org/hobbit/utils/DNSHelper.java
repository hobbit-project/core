package org.hobbit.utils;

import org.hobbit.utils.docker.DockerHelper;
import org.hobbit.utils.kubernetes.KubeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class DNSHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DNSHelper.class);
    private static final String KUBERNETES = "kubernetes";

    public static String getDNSName() {
        String runOn = getEnvVariable("RUN_ON");

        if (KUBERNETES.equalsIgnoreCase(runOn)) {
            LOGGER.info("Kubernetes is enabled");
            return KubeHelper.getDnsFriendlyIP();
        } else {
            LOGGER.info("Docker is enabled");
            return DockerHelper.getHost();
        }
    }

    private static String getEnvVariable(String key) {
        return Optional.ofNullable(System.getenv(key)).orElse("").trim();
    }
}
