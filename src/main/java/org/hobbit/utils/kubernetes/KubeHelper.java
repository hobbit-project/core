package org.hobbit.utils.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class KubeHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubeHelper.class);
    /**
     * Gets dns friendly ip
     *
     * @return plain hostname (or address)
     */
    public static String getDnsFriendlyIP(String namespace) {
        if(namespace == null) {
            namespace = "default";
        }
        String ip =  getPodIP();
        if(!ip.equals("unknown")&&ip!=null) {
            String dnsFriendlyIp = ip.replace(".","-")+namespace+".pod.cluster.local";
            return dnsFriendlyIp;
        }else {
            LOGGER.error("Error getting pod IP: {} namespace: {}", ip, namespace);
            return "unknown";
        }
    }

    public static String getPodIP() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            return ip.getHostAddress();
        } catch (UnknownHostException e) {
            // Handle the exception, e.g., log an error or return a default value.
            LOGGER.error("Error getting pod IP: " + e.getMessage());
            return "unknown"; // Or throw a RuntimeException, etc.
        }
    }
}
