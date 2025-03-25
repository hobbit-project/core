package org.hobbit.utils.kubernetes;

import org.junit.Test;
import static org.junit.Assert.*;

public class KubeHelperTest {

    @Test
    public void testGetPodIP() {
        String podIP = KubeHelper.getPodIP();
        assertNotNull(podIP);
        assertFalse(podIP.isEmpty());
    }

    @Test
    public void testGetDnsFriendlyIP() {
        // Ensure getPodIP() returns something valid before testing
        String podIP = KubeHelper.getPodIP();
        String namespace = KubeHelper.getEnvVariable("POD_NAMESPACE");
        if(namespace == null || "".equals(namespace)) {
            namespace = "default";
        }
        if (!podIP.equals("unknown")) {
            String dnsFriendlyIP = KubeHelper.getDnsFriendlyIP();
            assertTrue(dnsFriendlyIP.contains(namespace));  // Must include the namespace
            assertTrue(dnsFriendlyIP.contains("-"));  // IP should be hyphenated
            assertTrue(dnsFriendlyIP.contains(".pod.cluster.local"));
        } else {
            // If getPodIP() fails, the method should throw an exception
            try {
                KubeHelper.getDnsFriendlyIP();
                fail("Expected IllegalStateException due to unknown IP");
            } catch (IllegalStateException e) {
                // Expected behavior
            }
        }
    }
}
