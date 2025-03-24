package org.hobbit.utils;

import org.hobbit.utils.docker.DockerHelper;
import org.hobbit.utils.kubernetes.KubeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DNSHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DNSHelper.class);
    public static String getDNSName() {
        // determine if it is in docker or Kubernetes
        String isKubernetes = System.getenv("IS_KUBERNETES");
        if(isKubernetes != null && isKubernetes.equalsIgnoreCase("true")) {
            // kubernetes
            return KubeHelper.getDnsFriendlyIP();
        }else{
            // docker
            return DockerHelper.getHost();
        }
    }
}
