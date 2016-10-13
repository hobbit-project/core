package org.hobbit.core.rabbit;

import java.util.Random;

import org.apache.commons.io.Charsets;
import org.junit.Ignore;

@Ignore
public class RpcClientBasedEchoClient implements Runnable {

    private RabbitRpcClient client;
    private Random random;
    private int numberOfMessages;

    public RpcClientBasedEchoClient(RabbitRpcClient client, int numberOfMessages, long seed) {
        super();
        this.client = client;
        this.numberOfMessages = numberOfMessages;
        random = new Random(seed);
    }

    @Override
    public void run() {
        try {
            String msg, rsp;
            for (int i = 0; i < numberOfMessages; ++i) {
                msg = Integer.toString(random.nextInt());
                rsp = new String(client.request(msg.getBytes(Charsets.UTF_8)), Charsets.UTF_8);
                if (!msg.equals(rsp)) {
                    System.err.println("Message \"" + msg + "\" and response \"" + rsp + "\" are not equal!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
